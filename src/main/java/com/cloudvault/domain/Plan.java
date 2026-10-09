package com.cloudvault.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    /** Total storage quota in bytes for this plan. */
    @Column(name = "storage_bytes", nullable = false)
    private long storageBytes;

    /** Maximum single-file size in bytes. */
    @Column(name = "max_file_bytes", nullable = false)
    private long maxFileBytes;

    @Column(name = "sharing_enabled", nullable = false)
    private boolean sharingEnabled;

    @Column(name = "versioning_enabled", nullable = false)
    private boolean versioningEnabled;

    @Column(name = "max_versions", nullable = false)
    private int maxVersions = 1;

    @Column(name = "api_access", nullable = false)
    private boolean apiAccess = true;

    /** Trash retention in days. */
    @Column(name = "retention_days", nullable = false)
    private int retentionDays = 7;

    @Column(name = "max_downloads_per_day")
    private Integer maxDownloadsPerDay;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getStorageBytes() { return storageBytes; }
    public void setStorageBytes(long storageBytes) { this.storageBytes = storageBytes; }
    public long getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(long maxFileBytes) { this.maxFileBytes = maxFileBytes; }
    public boolean isSharingEnabled() { return sharingEnabled; }
    public void setSharingEnabled(boolean sharingEnabled) { this.sharingEnabled = sharingEnabled; }
    public boolean isVersioningEnabled() { return versioningEnabled; }
    public void setVersioningEnabled(boolean versioningEnabled) { this.versioningEnabled = versioningEnabled; }
    public int getMaxVersions() { return maxVersions; }
    public void setMaxVersions(int maxVersions) { this.maxVersions = maxVersions; }
    public boolean isApiAccess() { return apiAccess; }
    public void setApiAccess(boolean apiAccess) { this.apiAccess = apiAccess; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public Integer getMaxDownloadsPerDay() { return maxDownloadsPerDay; }
    public void setMaxDownloadsPerDay(Integer maxDownloadsPerDay) { this.maxDownloadsPerDay = maxDownloadsPerDay; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
