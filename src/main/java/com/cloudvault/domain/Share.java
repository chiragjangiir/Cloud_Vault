package com.cloudvault.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shares")
public class Share {

    public enum Type { PUBLIC_LINK, USER_SHARE }
    public enum Permission { VIEW, DOWNLOAD, EDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id")
    private FileEntry file;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Permission permission = Permission.VIEW;

    /** Cryptographically random, URL-safe token for public links only. */
    @Column(length = 64)
    private String token;

    @ManyToOne()
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "download_count", nullable = false)
    private int downloadCount;

    @ManyToOne()
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public boolean isRevoked() { return revokedAt != null; }

    /** Display status computed from real state at request time. */
    public String getStatusName() {
        if (isRevoked()) return "REVOKED";
        if (isExpired(Instant.now())) return "EXPIRED";
        if (maxDownloads != null && downloadCount >= maxDownloads) return "EXHAUSTED";
        return "ACTIVE";
    }

    public boolean isExpired(Instant now) { return expiresAt != null && !now.isBefore(expiresAt); }

    public boolean isUsable(Instant now) {
        if (isRevoked() || isExpired(now)) return false;
        if (maxDownloads != null && downloadCount >= maxDownloads) return false;
        return true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FileEntry getFile() { return file; }
    public void setFile(FileEntry file) { this.file = file; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Permission getPermission() { return permission; }
    public void setPermission(Permission permission) { this.permission = permission; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
