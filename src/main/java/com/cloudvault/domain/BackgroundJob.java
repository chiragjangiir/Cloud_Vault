package com.cloudvault.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "background_jobs")
public class BackgroundJob {

    public enum State { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 48)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private State state = State.QUEUED;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "progress_processed", nullable = false)
    private long progressProcessed;

    @Column(name = "progress_total", nullable = false)
    private long progressTotal;

    @Column(name = "bytes_processed", nullable = false)
    private long bytesProcessed;

    @Column(name = "bytes_total", nullable = false)
    private long bytesTotal;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @ManyToOne()
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public double progressPercent() {
        if (progressTotal <= 0) return state == State.COMPLETED ? 100.0 : 0.0;
        return Math.min(100.0, (progressProcessed * 100.0) / progressTotal);
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public long getProgressProcessed() { return progressProcessed; }
    public void setProgressProcessed(long progressProcessed) { this.progressProcessed = progressProcessed; }
    public long getProgressTotal() { return progressTotal; }
    public void setProgressTotal(long progressTotal) { this.progressTotal = progressTotal; }
    public long getBytesProcessed() { return bytesProcessed; }
    public void setBytesProcessed(long bytesProcessed) { this.bytesProcessed = bytesProcessed; }
    public long getBytesTotal() { return bytesTotal; }
    public void setBytesTotal(long bytesTotal) { this.bytesTotal = bytesTotal; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
