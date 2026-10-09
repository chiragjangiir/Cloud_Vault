package com.cloudvault.service;

import com.cloudvault.domain.FileEntry;
import com.cloudvault.domain.FileVersion;
import com.cloudvault.domain.Folder;
import com.cloudvault.domain.Share;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.repository.FileEntryRepository;
import com.cloudvault.repository.FolderRepository;
import com.cloudvault.repository.ShareRepository;
import com.cloudvault.repository.SubscriptionRepository;
import com.cloudvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Produces a genuine archive of the user's data: a JSON manifest with every
 * folder, file and share (tokens excluded — they are credentials), plus the
 * real bytes of every file's active version streamed from storage.
 */
@Service
public class ExportService {

    private final UserRepository users;
    private final FolderRepository folders;
    private final FileEntryRepository files;
    private final ShareRepository shares;
    private final SubscriptionRepository subscriptions;
    private final FileService fileService;
    private final ObjectStorageService objectStorage;

    public ExportService(UserRepository users, FolderRepository folders, FileEntryRepository files,
                         ShareRepository shares, SubscriptionRepository subscriptions,
                         FileService fileService, ObjectStorageService objectStorage) {
        this.users = users;
        this.folders = folders;
        this.files = files;
        this.shares = shares;
        this.subscriptions = subscriptions;
        this.fileService = fileService;
        this.objectStorage = objectStorage;
    }

    @Transactional(readOnly = true)
    public void exportUser(User user, OutputStream out) throws IOException {
        Map<Long, String> folderPaths = buildFolderPaths(user);
        List<FileEntry> allFiles = files.findLiveByOwnerRecent(user.getId());

        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            // Manifest
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("exportedAt", java.time.Instant.now().toString());
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("displayName", user.getDisplayName());
            profile.put("createdAt", user.getCreatedAt().toString());
            manifest.put("profile", profile);

            subscriptions.findByUserId(user.getId()).ifPresent(sub -> {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("plan", sub.getPlan().getCode());
                s.put("status", sub.getStatus().name());
                s.put("quotaBytes", sub.getStorageBytesOverride() != null
                        ? sub.getStorageBytesOverride() : sub.getPlan().getStorageBytes());
                manifest.put("subscription", s);
            });

            manifest.put("folders", folderPaths.values().stream().sorted().toList());

            List<Map<String, Object>> fileRows = allFiles.stream().map(f -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("path", folderPaths.get(f.getFolder().getId()) + "/" + f.getName());
                row.put("size", f.getSizeBytes());
                row.put("contentType", f.getContentType());
                row.put("sha256", f.getChecksumSha256());
                row.put("createdAt", f.getCreatedAt().toString());
                row.put("updatedAt", f.getUpdatedAt().toString());
                row.put("versions", f.getVersionCount());
                return row;
            }).toList();
            manifest.put("files", fileRows);

            List<Map<String, Object>> shareRows = shares.findByOwnerIdWithFile(user.getId()).stream().map(s -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("file", s.getFile().getName());
                row.put("type", s.getType().name());
                row.put("permission", s.getPermission().name());
                row.put("createdAt", s.getCreatedAt().toString());
                row.put("expiresAt", s.getExpiresAt() == null ? null : s.getExpiresAt().toString());
                row.put("revoked", s.isRevoked());
                row.put("downloadCount", s.getDownloadCount());
                return row;
            }).toList();
            manifest.put("shares", shareRows);

            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(Json.pretty(manifest).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();

            // Real file bytes
            for (FileEntry file : allFiles) {
                FileVersion version = file.getActiveVersion();
                if (version == null) continue;
                String path = folderPaths.get(file.getFolder().getId()) + "/" + file.getName();
                zip.putNextEntry(new ZipEntry("data" + path));
                try (InputStream in = objectStorage.openRead(version.getStorageObject(), 0, -1)) {
                    in.transferTo(zip);
                } catch (IOException e) {
                    throw new IOException("Failed to export \"" + file.getName() + "\": " + e.getMessage(), e);
                }
                zip.closeEntry();
            }
        }
        // NOTE: the audit entry is written by the controller AFTER this method
        // returns — this method runs in a read-only transaction and must stay
        // read-only so large exports stream without holding write resources.
    }

    /** Maps every folder id to its absolute path string (sanitized by construction). */
    private Map<Long, String> buildFolderPaths(User user) {
        List<Folder> all = folders.findAll().stream()
                .filter(f -> f.getOwner().getId().equals(user.getId()))
                .toList();
        Map<Long, Folder> byId = new HashMap<>();
        for (Folder f : all) byId.put(f.getId(), f);
        Map<Long, String> paths = new HashMap<>();
        for (Folder f : all) {
            paths.put(f.getId(), buildPath(f, byId));
        }
        return paths;
    }

    private String buildPath(Folder folder, Map<Long, Folder> byId) {
        StringBuilder sb = new StringBuilder();
        Folder cursor = folder;
        int guard = 0;
        while (cursor != null && guard++ < 128) {
            sb.insert(0, "/" + sanitizeSegment(cursor.getName()));
            cursor = cursor.getParent() == null ? null : byId.get(cursor.getParent().getId());
        }
        String path = sb.toString();
        return path.isEmpty() ? "" : path;
    }

    private static String sanitizeSegment(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
    }
}
