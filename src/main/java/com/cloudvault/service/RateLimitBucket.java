package com.cloudvault.service;

public class RateLimitBucket {
    private final long capacity;
    private long tokens;

    public RateLimitBucket(long capacity) {
        this.capacity = capacity;
        this.tokens = capacity;
    }

    public synchronized boolean consume(long tokensRequested) {
        if (tokensRequested <= 0) return true;
        if (tokens >= tokensRequested) {
            tokens -= tokensRequested;
            return true;
        }
        return false;
    }

    public synchronized void refill(long amount) {
        this.tokens = Math.min(capacity, this.tokens + amount);
    }

    public synchronized long getTokens() {
        return tokens;
    }
}
