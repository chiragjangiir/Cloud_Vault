package com.cloudvault.service;

import com.cloudvault.domain.FileEntry;
import com.cloudvault.domain.Share;
import com.cloudvault.domain.User;
import com.cloudvault.repository.FileEntryRepository;
import com.cloudvault.repository.ShareRepository;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.web.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Real file sharing: PUBLIC_LINK shares carry a cryptographically random
 * token; USER_SHARE shares map to a specific recipient. Expiration, revocation
 * and download limits are enforced server-side on every access.
 */
@Service
public class ShareService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShareRepository shares;
    private final FileEntryRepository files;
    private final UserRepository users;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final com.cloudvault.config.AppProperties props;

    public ShareService(ShareRepository shares, FileEntryRepository files, UserRepository users,
                        SubscriptionService subscriptionService, NotificationService notificationService,
                        AuditService auditService, com.cloudvault.config.AppProperties props) {
        this.shares = shares;
        this.files = files;
        this.users = users;
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.props = props;
    }

    private FileEntry ownedFile(User owner, long fileId) {
        FileEntry file = files.findByIdWithFolder(fileId)
                .orElseThrow(() -> ApiException.notFound("File not found"));
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw ApiException.notFound("File not found");
        }
        if (file.getStatus() != FileEntry.Status.ACTIVE) {
            throw ApiException.conflict("Files in the trash cannot be shared");
        }
        return file;
    }

    @Transactional
    public Share createPublicLink(User owner, long fileId, Share.Permission permission,
                                  Instant expiresAt, Integer maxDownloads) {
        subscriptionService.requireSharing(owner);
        FileEntry file = ownedFile(owner, fileId);
        Share share = new Share();
        share.setFile(file);
        share.setOwner(owner);
        share.setType(Share.Type.PUBLIC_LINK);
        share.setPermission(permission);
        share.setToken(randomToken());
        share.setExpiresAt(expiresAt);
        share.setMaxDownloads(maxDownloads);
        share.setCreatedBy(owner);
        share = shares.save(share);
        // The token itself is never logged or put into audit details.
        auditService.record("SHARE_CREATED", "SHARE", String.valueOf(share.getId()),
                "public link for file " + file.getId() + " (" + permission + ")");
        notificationService.notify(owner, NotificationService.SHARE_CREATED,
                "Public link created", "A " + permission + " link was created for \"" + file.getName() + "\".",
                "/app/shares");
        return share;
    }

    @Transactional
    public Share createUserShare(User owner, long fileId, String recipientUsername,
                                 Share.Permission permission, Instant expiresAt) {
        subscriptionService.requireSharing(owner);
        FileEntry file = ownedFile(owner, fileId);
        User recipient = users.findByUsernameIgnoreCase(recipientUsername)
                .orElseThrow(() -> ApiException.notFound("No user with that username"));
        if (recipient.getId().equals(owner.getId())) {
            throw ApiException.unprocessable("You already own this file");
        }
        boolean duplicate = shares.findByFileId(fileId).stream().anyMatch(s ->
                s.getType() == Share.Type.USER_SHARE && s.getRecipient() != null
                        && s.getRecipient().getId().equals(recipient.getId())
                        && s.getRevokedAt() == null);
        if (duplicate) {
            throw ApiException.conflict("That user already has access to this file");
        }
        Share share = new Share();
        share.setFile(file);
        share.setOwner(owner);
        share.setType(Share.Type.USER_SHARE);
        share.setPermission(permission);
        share.setRecipient(recipient);
        share.setExpiresAt(expiresAt);
        share.setCreatedBy(owner);
        share = shares.save(share);
        auditService.record("SHARE_CREATED", "SHARE", String.valueOf(share.getId()),
                "user share of file " + file.getId() + " to " + recipient.getUsername() + " (" + permission + ")");
        notificationService.notify(recipient, NotificationService.SHARE_CREATED,
                "A file was shared with you",
                owner.getUsername() + " shared \"" + file.getName() + "\" with you (" + permission + ").",
                "/app/shares");
        return share;
    }

    @Transactional
    public void revoke(User owner, long shareId) {
        Share share = shares.findById(shareId)
                .orElseThrow(() -> ApiException.notFound("Share not found"));
        if (!share.getOwner().getId().equals(owner.getId())) {
            throw ApiException.notFound("Share not found");
        }
        if (share.getRevokedAt() != null) return;
        share.setRevokedAt(Instant.now());
        shares.save(share);
        auditService.record("SHARE_REVOKED", "SHARE", String.valueOf(shareId), "file " + share.getFile().getId());
    }

    @Transactional(readOnly = true)
    public List<Share> ownedBy(User owner) {
        return shares.findByOwnerIdWithFile(owner.getId());
    }

    @Transactional(readOnly = true)
    public List<Share> sharedWith(User user) {
        return shares.findByRecipientIdWithFile(user.getId());
    }

    /** Resolves a public token to its share; enforces usability (403 when expired/revoked). */
    @Transactional(readOnly = true)
    public Share resolvePublic(String token) {
        if (token == null || token.isBlank() || token.length() > 64) {
            throw ApiException.notFound("Invalid share link");
        }
        Share share = shares.findByToken(token)
                .orElseThrow(() -> ApiException.notFound("Invalid share link"));
        if (!share.isUsable(Instant.now())) {
            throw ApiException.forbidden("This share link has expired or has been revoked");
        }
        if (share.getFile().getStatus() != FileEntry.Status.ACTIVE) {
            throw ApiException.forbidden("This share link has expired or has been revoked");
        }
        return share;
    }

    public String publicUrl(String token) {
        return props.getPublicUrl() + "/share/" + token;
    }

    private static String randomToken() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
