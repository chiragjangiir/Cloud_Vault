package com.cloudvault.service;

import com.cloudvault.domain.FileEntry;
import com.cloudvault.domain.FileVersion;
import com.cloudvault.domain.Folder;
import com.cloudvault.domain.QuotaReservation;
import com.cloudvault.domain.Share;
import com.cloudvault.domain.StorageLocation;
import com.cloudvault.domain.StorageObject;
import com.cloudvault.domain.StoragePool;
import com.cloudvault.domain.User;
import com.cloudvault.repository.FileEntryRepository;
import com.cloudvault.repository.FileVersionRepository;
import com.cloudvault.repository.ShareRepository;
import com.cloudvault.service.storage.StorageProvider;
import com.cloudvault.web.error.ApiException;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Real file lifecycle: streaming upload with quota reservation, verified
 * storage, ranged download with authorization, trash/restore/purge, versioning,
 * rename/move and database-backed search.
 *
 * <p>Physical IO happens outside database transactions; metadata + quota are
 * committed together in one transaction after the object is written and
 * verified. Any failure deletes the physical object and releases the quota
 * reservation, so the database never claims data that isn't there.</p>
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileEntryRepository files;
    private final FileVersionRepository versions;
    private final ShareRepository shares;
    private final FolderService folderService;
    private final QuotaService quotaService;
    private final SubscriptionService subscriptionService;
    private final StoragePoolService poolService;
    private final ObjectStorageService objectStorage;
    private final AuditService auditService;
    private final TransactionTemplate tx;

    public FileService(FileEntryRepository files,
                       FileVersionRepository versions,
                       ShareRepository shares,
                       FolderService folderService,
                       QuotaService quotaService,
                       SubscriptionService subscriptionService,
                       StoragePoolService poolService,
                       ObjectStorageService objectStorage,
                       AuditService auditService,
                       PlatformTransactionManager txManager) {
        this.files = files;
        this.versions = versions;
        this.shares = shares;
        this.folderService = folderService;
        this.quotaService = quotaService;
        this.subscriptionService = subscriptionService;
        this.poolService = poolService;
        this.objectStorage = objectStorage;
        this.auditService = auditService;
        this.tx = new TransactionTemplate(txManager);
    }

    // ------------------------------------------------------------------
    // Upload
    // ------------------------------------------------------------------

    /**
     * Full upload pipeline: authenticate/authorize (caller), subscription
     * check, quota reservation, placement, streamed write with checksum,
     * transactional metadata commit, reservation release on failure.
     */
    public FileEntry upload(User user, Long folderId, String rawName, String contentType,
                            InputStream in, long declaredSize) {
        subscriptionService.requireNotSuspended(user);
        String name = FolderService.sanitizeName(rawName);
        if (declaredSize < 0) {
            throw ApiException.unprocessable("Content-Length is required");
        }
        Folder folder = folderService.resolveLive(user, folderId);
        QuotaReservation reservation = quotaService.reserve(user, declaredSize, "upload");
        try {
            FileEntry file = writeAndCommit(user, folder, name, contentType, in, reservation);
            auditService.record("FILE_UPLOADED", "FILE", String.valueOf(file.getId()),
                    name + " (" + file.getSizeBytes() + " bytes, sha256 " + shortHash(file.getChecksumSha256()) + ")");
            return file;
        } catch (RuntimeException e) {
            quotaService.release(reservation.getId());
            throw e;
        }
    }

    private FileEntry writeAndCommit(User user, Folder folder, String name, String contentType,
                                     InputStream in, QuotaReservation reservation) {
        StoragePoolService.Placement placement = poolService.pick(reservation.getBytes());
        String key = objectStorage.provider().newStorageKey();
        try {
            StorageProvider.WriteResult written =
                    objectStorage.writePhysical(placement.location(), key, in);
            CommitResult result = tx.execute(status ->
                    commitUpload(user, folder, name, contentType, placement, key, written, reservation));
            if (result == null) throw new ApiException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED", "Upload commit failed");
            purgeAfterCommit(result.purge());
            return result.file();
        } catch (RuntimeException e) {
            objectStorage.deleteByKeyQuietly(placement.location(), key);
            throw e;
        }
    }

    private CommitResult commitUpload(User user, Folder folder, String name, String contentType,
                                      StoragePoolService.Placement placement, String key,
                                      StorageProvider.WriteResult written, QuotaReservation reservation) {
        List<FileEntry> live = files.findLiveInFolder(user.getId(), folder.getId());
        FileEntry existing = live.stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        StorageObject object = objectStorage.persist(placement.pool(), placement.location(), key, written, contentType);
        List<StorageObject> purge = new ArrayList<>();

        if (existing == null) {
            FileEntry file = new FileEntry();
            file.setOwner(user);
            file.setFolder(folder);
            file.setName(name);
            file.setContentType(contentType);
            file.setSizeBytes(written.sizeBytes());
            file.setChecksumSha256(written.checksumSha256());
            file.setStatus(FileEntry.Status.ACTIVE);
            file = files.save(file);

            FileVersion version = new FileVersion();
            version.setFile(file);
            version.setVersionNumber(1);
            version.setSizeBytes(written.sizeBytes());
            version.setChecksumSha256(written.checksumSha256());
            version.setContentType(contentType);
            version.setStorageObject(object);
            version.setCreatedBy(user);
            version = versions.save(version);

            file.setActiveVersion(version);
            file.setVersionCount(1);
            file = files.save(file);
            quotaService.commit(reservation, written.sizeBytes());
            return new CommitResult(file, purge);
        }

        // Replace existing file with a new version (real versioning).
        if (!folder.getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("Not allowed");
        }
        List<FileVersion> prior = versions.findByFileIdOrderByVersionNumberDesc(existing.getId());
        int nextNumber = prior.isEmpty() ? 1 : prior.get(0).getVersionNumber() + 1;
        FileVersion version = new FileVersion();
        version.setFile(existing);
        version.setVersionNumber(nextNumber);
        version.setSizeBytes(written.sizeBytes());
        version.setChecksumSha256(written.checksumSha256());
        version.setContentType(contentType);
        version.setStorageObject(object);
        version.setCreatedBy(user);
        version = versions.save(version);

        existing.setActiveVersion(version);
        existing.setSizeBytes(written.sizeBytes());
        existing.setChecksumSha256(written.checksumSha256());
        existing.setContentType(contentType);
        existing.setVersionCount(prior.size() + 1);
        existing.touch();
        existing = files.save(existing);
        quotaService.commit(reservation, written.sizeBytes());

        // Prune history down to the plan's allowed version count.
        SubscriptionService.Limits limits = quotaService.limitsFor(user);
        List<FileVersion> pruned;
        if (!limits.versioningEnabled() || limits.maxVersions() <= 1) {
            pruned = prior;
        } else if (prior.size() + 1 > limits.maxVersions()) {
            pruned = prior.subList(0, prior.size() + 1 - limits.maxVersions());
        } else {
            pruned = List.of();
        }
        if (!pruned.isEmpty()) {
            long freed = pruned.stream().mapToLong(FileVersion::getSizeBytes).sum();
            for (FileVersion v : pruned) {
                purge.add(v.getStorageObject());
            }
            versions.deleteAll(pruned);
            existing.setVersionCount(existing.getVersionCount() - pruned.size());
            existing = files.save(existing);
            quotaService.deduct(user, freed);
        }
        return new CommitResult(existing, purge);
    }

    private void purgeAfterCommit(List<StorageObject> objects) {
        if (objects.isEmpty()) return;
        List<Long> ids = objects.stream().map(StorageObject::getId).toList();
        try {
            for (StorageObject obj : objects) {
                objectStorage.deletePhysical(obj);
            }
        } catch (IOException e) {
            // Rows are already gone; the leftover files will be reported by the
            // reconciliation audit as physical orphans. Never swallow silently.
            log.error("Failed to delete {} pruned object(s) after version prune: {}", objects.size(), e.getMessage());
        }
        objectStorage.deleteMetadata(ids);
    }

    private record CommitResult(FileEntry file, List<StorageObject> purge) {}

    /**
     * Uploads a new version on behalf of a share recipient with EDIT
     * permission. Quota is charged to the file owner.
     */
    public FileEntry uploadVersionByShare(Share share, User actor, String contentType,
                                          InputStream in, long declaredSize) {
        FileEntry file = share.getFile();
        if (file.getStatus() != FileEntry.Status.ACTIVE) {
            throw ApiException.notFound("File not found");
        }
        if (share.getPermission() != Share.Permission.EDIT || !share.isUsable(Instant.now())) {
            throw ApiException.forbidden("This share does not allow editing");
        }
        User owner = file.getOwner();
        subscriptionService.requireNotSuspended(owner);
        QuotaReservation reservation = quotaService.reserve(owner, declaredSize, "share-edit");
        try {
            FileEntry updated = writeAndCommit(owner, file.getFolder(), file.getName(), contentType, in, reservation);
            auditService.record("FILE_VERSION_BY_SHARE", "FILE", String.valueOf(file.getId()),
                    "edited via share #" + share.getId() + " by " + actor.getUsername());
            return updated;
        } catch (RuntimeException e) {
            quotaService.release(reservation.getId());
            throw e;
        }
    }

    // ------------------------------------------------------------------
    // Download
    // ------------------------------------------------------------------

    public record Download(FileEntry file, FileVersion version, StorageObject object, Share viaShare) {}

    public Download prepareDownload(User viewer, long fileId, String shareToken) {
        FileEntry file = files.findByIdWithFolder(fileId)
                .orElseThrow(() -> ApiException.notFound("File not found"));
        if (file.getStatus() != FileEntry.Status.ACTIVE) {
            throw ApiException.notFound("File not found");
        }
        Share share = authorize(file, viewer, Share.Permission.DOWNLOAD, shareToken);
        FileVersion version = file.getActiveVersion();
        if (version == null) {
            throw ApiException.conflict("File has no stored content");
        }
        if (share != null) {
            share.setDownloadCount(share.getDownloadCount() + 1);
            shares.save(share);
        }
        return new Download(file, version, version.getStorageObject(), share);
    }

    /** Metadata access (VIEW) for owner or share recipients. */
    public FileEntry authorizeView(User viewer, long fileId, String shareToken) {
        FileEntry file = files.findByIdWithFolder(fileId)
                .orElseThrow(() -> ApiException.notFound("File not found"));
        if (file.getStatus() != FileEntry.Status.ACTIVE) throw ApiException.notFound("File not found");
        authorize(file, viewer, Share.Permission.VIEW, shareToken);
        return file;
    }

    /**
     * Resolves the share granting {@code required} access, or null when the
     * viewer owns the file. Unknown files → 404; unusable shares → 403.
     */
    private Share authorize(FileEntry file, User viewer, Share.Permission required, String shareToken) {
        boolean owner = viewer != null && Objects.equals(file.getOwner().getId(), viewer.getId());
        if (owner) return null;
        List<Share> candidates = shares.findByFileId(file.getId());
        Share matched = null;
        Instant now = Instant.now();
        for (Share s : candidates) {
            if (s.getType() == Share.Type.PUBLIC_LINK && shareToken != null && s.getToken() != null
                    && constantTimeEquals(s.getToken(), shareToken)) {
                matched = s;
                break;
            }
            if (s.getType() == Share.Type.USER_SHARE && viewer != null && s.getRecipient() != null
                    && Objects.equals(s.getRecipient().getId(), viewer.getId())) {
                matched = s;
                break;
            }
        }
        if (matched == null) {
            throw ApiException.notFound("File not found");
        }
        if (!matched.isUsable(now)) {
            throw ApiException.forbidden("This share link has expired or has been revoked");
        }
        if (required == Share.Permission.DOWNLOAD && matched.getPermission() == Share.Permission.VIEW) {
            throw ApiException.forbidden("This share does not allow downloading");
        }
        if (required == Share.Permission.EDIT && matched.getPermission() != Share.Permission.EDIT) {
            throw ApiException.forbidden("This share does not allow editing");
        }
        return matched;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // Trash / restore / purge
    // ------------------------------------------------------------------

    @org.springframework.transaction.annotation.Transactional
    public void trash(User user, long fileId) {
        FileEntry file = requireOwned(user, fileId);
        if (file.getStatus() == FileEntry.Status.TRASHED) {
            throw ApiException.conflict("File is already in the trash");
        }
        SubscriptionService.Limits limits = quotaService.limitsFor(user);
        Instant now = Instant.now();
        file.setStatus(FileEntry.Status.TRASHED);
        file.setDeletedAt(now);
        file.setRetentionUntil(now.plusSeconds(limits.retentionDays() * 86400L));
        file.setOriginalFolderId(file.getFolder().getId());
        file.touch();
        files.save(file);
        auditService.record("FILE_TRASHED", "FILE", String.valueOf(file.getId()), file.getName());
    }

    @org.springframework.transaction.annotation.Transactional
    public void restore(User user, long fileId) {
        FileEntry file = files.findByIdWithFolder(fileId)
                .orElseThrow(() -> ApiException.notFound("File not found"));
        if (!file.getOwner().getId().equals(user.getId())) throw ApiException.notFound("File not found");
        if (file.getStatus() != FileEntry.Status.TRASHED) {
            throw ApiException.conflict("File is not in the trash");
        }
        // Restore into the original folder when it is still live, else root.
        Folder target = folderService.ensureRoot(user);
        if (file.getOriginalFolderId() != null) {
            Folder candidate = folderService.resolveLiveOrNull(user, file.getOriginalFolderId());
            if (candidate != null) target = candidate;
        }
        // Auto-rename when the name has been taken since trashing.
        String name = file.getName();
        if (nameTaken(target, name)) {
            name = withSuffix(file.getName(), "restored");
            int i = 2;
            while (nameTaken(target, name)) {
                name = withSuffix(file.getName(), "restored " + i);
                i++;
            }
            file.setName(name);
        }
        file.setFolder(target);
        file.setStatus(FileEntry.Status.ACTIVE);
        file.setDeletedAt(null);
        file.setRetentionUntil(null);
        file.setOriginalFolderId(null);
        file.touch();
        files.save(file);
        auditService.record("FILE_RESTORED", "FILE", String.valueOf(file.getId()), name);
    }

    /**
     * Permanently deletes a file: physical objects first (verified), then
     * metadata rows, then quota deduction. Failures are reported, never
     * swallowed — the reconciliation audit covers any partial state.
     */
    @org.springframework.transaction.annotation.Transactional
    public void purge(User user, long fileId) {
        FileEntry file = files.findByIdWithFolder(fileId)
                .orElseThrow(() -> ApiException.notFound("File not found"));
        if (!file.getOwner().getId().equals(user.getId())) throw ApiException.notFound("File not found");
        purgeInternal(file);
    }

    /** Purge used by background cleanup (no interactive user). */
    @org.springframework.transaction.annotation.Transactional
    public void purgeInternal(FileEntry file) {
        List<FileVersion> all = versions.findByFileIdOrderByVersionNumberDesc(file.getId());
        long freed = all.stream().mapToLong(FileVersion::getSizeBytes).sum();
        List<Long> objectIds = new ArrayList<>();
        IOException failure = null;
        for (FileVersion v : all) {
            StorageObject obj = v.getStorageObject();
            objectIds.add(obj.getId());
            try {
                objectStorage.deletePhysical(obj);
            } catch (IOException e) {
                failure = e;
                log.error("Failed to delete physical object for file {}: {}", file.getId(), e.getMessage());
            }
        }
        if (failure != null) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "STORAGE_DELETE_FAILED", "Could not fully delete file from storage: " + failure.getMessage());
        }
        // Delete version rows in the persistence context as well: leaving them
        // managed while their FileEntry is removed fails Hibernate's flush-time
        // association check (the DB schema would cascade anyway, but JPA must
        // agree with the database). active_version_id is ON DELETE SET NULL.
        versions.deleteAll(all);
        files.delete(file);            // DB cascades any remaining dependents
        objectStorage.deleteMetadata(objectIds);
        quotaService.deduct(file.getOwner(), freed);
        auditService.record("FILE_PURGED", "FILE", String.valueOf(file.getId()), file.getName());
    }

    // ------------------------------------------------------------------
    // Rename / move / versions
    // ------------------------------------------------------------------

    @org.springframework.transaction.annotation.Transactional
    public FileEntry rename(User user, long fileId, String rawName) {
        String name = FolderService.sanitizeName(rawName);
        FileEntry file = requireOwned(user, fileId);
        Folder folder = file.getFolder();
        if (!name.equalsIgnoreCase(file.getName()) && nameTaken(folder, name)) {
            throw ApiException.conflict("An item with that name already exists here");
        }
        String old = file.getName();
        file.setName(name);
        file.touch();
        file = files.save(file);
        auditService.record("FILE_RENAMED", "FILE", String.valueOf(file.getId()), old + " -> " + name);
        return file;
    }

    @org.springframework.transaction.annotation.Transactional
    public FileEntry move(User user, long fileId, Long targetFolderId) {
        FileEntry file = requireOwned(user, fileId);
        Folder target = folderService.resolveLive(user, targetFolderId);
        if (nameTaken(target, file.getName())) {
            throw ApiException.conflict("An item with that name already exists in the destination");
        }
        file.setFolder(target);
        file.touch();
        file = files.save(file);
        auditService.record("FILE_MOVED", "FILE", String.valueOf(file.getId()), "moved to folder " + target.getId());
        return file;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<FileVersion> listVersions(User user, long fileId) {
        FileEntry file = requireOwned(user, fileId);
        return versions.findByFileIdOrderByVersionNumberDesc(file.getId());
    }

    @org.springframework.transaction.annotation.Transactional
    public void activateVersion(User user, long fileId, int versionNumber) {
        FileEntry file = requireOwned(user, fileId);
        FileVersion target = versions.findByFileIdAndVersionNumber(file.getId(), versionNumber)
                .orElseThrow(() -> ApiException.notFound("Version not found"));
        file.setActiveVersion(target);
        file.setSizeBytes(target.getSizeBytes());
        file.setChecksumSha256(target.getChecksumSha256());
        file.setContentType(target.getContentType());
        file.touch();
        files.save(file);
        auditService.record("FILE_VERSION_ACTIVATED", "FILE", String.valueOf(file.getId()), "version " + versionNumber);
    }

    // ------------------------------------------------------------------
    // Reads / search
    // ------------------------------------------------------------------

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public FileEntry getOwned(User user, long fileId) {
        return requireOwned(user, fileId);
    }

    private FileEntry requireOwned(User user, long fileId) {
        FileEntry file = files.findByIdWithFolder(fileId)
                .orElseThrow(() -> ApiException.notFound("File not found"));
        if (!file.getOwner().getId().equals(user.getId())) {
            throw ApiException.notFound("File not found"); // no existence leak across users
        }
        return file;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<FileEntry> recent(User user) {
        return files.findLiveByOwnerRecent(user.getId());
    }

    public record SearchCriteria(String query, String extension, String mimeType, Long folderId,
                                 Instant from, Instant to, Long minSize, Long maxSize) {}

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<FileEntry> search(User user, SearchCriteria c, Pageable pageable) {
        Specification<FileEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("owner").get("id"), user.getId()));
            predicates.add(cb.equal(root.get("status"), FileEntry.Status.ACTIVE));
            if (c.query() != null && !c.query().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + c.query().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (c.extension() != null && !c.extension().isBlank()) {
                String ext = c.extension().trim().toLowerCase(Locale.ROOT);
                if (ext.startsWith(".")) ext = ext.substring(1);
                predicates.add(cb.like(cb.lower(root.get("name")), "%." + ext));
            }
            if (c.mimeType() != null && !c.mimeType().isBlank()) {
                if (c.mimeType().endsWith("/*")) {
                    predicates.add(cb.like(root.get("contentType"), c.mimeType().substring(0, c.mimeType().length() - 1) + "%"));
                } else {
                    predicates.add(cb.equal(root.get("contentType"), c.mimeType()));
                }
            }
            if (c.folderId() != null) {
                predicates.add(cb.equal(root.get("folder").get("id"), c.folderId()));
            }
            if (c.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.from()));
            if (c.to() != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), c.to()));
            if (c.minSize() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("sizeBytes"), c.minSize()));
            if (c.maxSize() != null) predicates.add(cb.lessThanOrEqualTo(root.get("sizeBytes"), c.maxSize()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return files.findAll(spec, pageable);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean nameTaken(Folder folder, String name) {
        boolean folderTaken = folderService.liveSiblingNames(folder.getOwner(), folder).stream()
                .anyMatch(n -> n.equalsIgnoreCase(name));
        if (folderTaken) return true;
        return files.findLiveInFolder(folder.getOwner().getId(), folder.getId()).stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(name));
    }

    private static String withSuffix(String name, String suffix) {
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            return name.substring(0, dot) + " (" + suffix + ")" + name.substring(dot);
        }
        return name + " (" + suffix + ")";
    }

    private static String shortHash(String hash) {
        return hash == null ? "unknown" : hash.substring(0, 12);
    }
}
