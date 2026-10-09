package com.cloudvault.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_usage")
public class UserUsage {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "used_bytes", nullable = false)
    private long usedBytes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public long getUsedBytes() { return usedBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
