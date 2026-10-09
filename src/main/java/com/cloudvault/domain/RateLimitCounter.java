package com.cloudvault.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "rate_limit_counters")
public class RateLimitCounter {

    @EmbeddedId
    private RateLimitId id;

    @Column(nullable = false)
    private int hits;

    @Column(name = "last_seen_at")
    private java.time.Instant lastSeenAt = java.time.Instant.now();

    public RateLimitCounter() {}

    public RateLimitId getId() { return id; }
    public void setId(RateLimitId id) { this.id = id; }
    public int getHits() { return hits; }
    public void setHits(int hits) { this.hits = hits; }
    public java.time.Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(java.time.Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    @Embeddable
    @org.hibernate.annotations.Immutable
    public static class RateLimitId implements java.io.Serializable {
        @Column(name = "bucket_key", length = 255)
        private String bucketKey;

        @Column(name = "window_start")
        private java.time.Instant windowStart;

        public RateLimitId() {}

        public RateLimitId(String bucketKey, java.time.Instant windowStart) {
            this.bucketKey = bucketKey;
            this.windowStart = windowStart;
        }

        public String getBucketKey() { return bucketKey; }
        public java.time.Instant getWindowStart() { return windowStart; }
    }
}
