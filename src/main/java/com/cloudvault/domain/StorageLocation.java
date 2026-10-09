package com.cloudvault.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "storage_locations")
public class StorageLocation {

    public enum Status { ONLINE, OFFLINE, DISABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "root_path", nullable = false, length = 1024)
    private String rootPath;

    @Column(nullable = false, length = 32)
    private String provider = "local-filesystem";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.OFFLINE;

    @Column(name = "total_capacity_bytes", nullable = false)
    private long totalCapacityBytes;

    @Column(name = "usable_capacity_bytes", nullable = false)
    private long usableCapacityBytes;

    @Column(length = 64)
    private String filesystem;

    @Column(name = "access_mode", nullable = false, length = 8)
    private String accessMode = "RW";

    @Column(name = "is_writable", nullable = false)
    private boolean writable;

    @Column(name = "last_health_check")
    private Instant lastHealthCheck;

    @Column(name = "last_health_message", length = 500)
    private String lastHealthMessage;

    @Column(name = "auto_registered", nullable = false)
    private boolean autoRegistered;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRootPath() { return rootPath; }
    public void setRootPath(String rootPath) { this.rootPath = rootPath; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public long getTotalCapacityBytes() { return totalCapacityBytes; }
    public void setTotalCapacityBytes(long totalCapacityBytes) { this.totalCapacityBytes = totalCapacityBytes; }
    public long getUsableCapacityBytes() { return usableCapacityBytes; }
    public void setUsableCapacityBytes(long usableCapacityBytes) { this.usableCapacityBytes = usableCapacityBytes; }
    public String getFilesystem() { return filesystem; }
    public void setFilesystem(String filesystem) { this.filesystem = filesystem; }
    public String getAccessMode() { return accessMode; }
    public void setAccessMode(String accessMode) { this.accessMode = accessMode; }
    public boolean isWritable() { return writable; }
    public void setWritable(boolean writable) { this.writable = writable; }
    public Instant getLastHealthCheck() { return lastHealthCheck; }
    public void setLastHealthCheck(Instant lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
    public String getLastHealthMessage() { return lastHealthMessage; }
    public void setLastHealthMessage(String lastHealthMessage) { this.lastHealthMessage = lastHealthMessage; }
    public boolean isAutoRegistered() { return autoRegistered; }
    public void setAutoRegistered(boolean autoRegistered) { this.autoRegistered = autoRegistered; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
