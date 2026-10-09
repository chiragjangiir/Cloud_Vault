package com.cloudvault.service;

import com.cloudvault.web.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Real fixed-window rate limiter backed by Postgres counters. The increment is
 * a single atomic upsert, so concurrent requests cannot bypass the limit.
 */
@Service
public class RateLimitService {

    private final JdbcTemplate jdbc;

    public RateLimitService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Decision(boolean allowed, int hits, int limit) {}

    private static Instant windowStart(Duration window) {
        Instant now = Instant.now();
        long millis = window.toMillis();
        long epoch = now.toEpochMilli();
        return Instant.ofEpochMilli((epoch / millis) * millis);
    }

    /** Atomically increments the counter and returns the resulting hit count. */
    public int increment(String bucket, String key, Duration window) {
        Instant start = windowStart(window);
        String bucketKey = bucket + ":" + key;
        Integer hits = jdbc.queryForObject("""
                INSERT INTO rate_limit_counters (bucket_key, window_start, hits)
                VALUES (?, ?, 1)
                ON CONFLICT (bucket_key, window_start)
                DO UPDATE SET hits = rate_limit_counters.hits + 1
                RETURNING hits
                """, Integer.class, bucketKey, Timestamp.from(start));
        return hits == null ? 1 : hits;
    }

    public Decision check(String bucket, String key, int limit, Duration window) {
        int hits = increment(bucket, key, window);
        return new Decision(hits <= limit, hits, limit);
    }

    /** Check-and-consume: throws 429 when the window is exhausted. */
    @Transactional
    public void consume(String bucket, String key, int limit, Duration window) {
        Decision d = check(bucket, key, limit, window);
        if (!d.allowed()) {
            throw ApiException.rateLimited("Too many requests; please retry later");
        }
    }

    /** Removes counters outside the retention window. Called by the cleanup job. */
    public int purgeOldWindows(Duration retention) {
        Timestamp cutoff = Timestamp.from(Instant.now().minus(retention));
        return jdbc.update("DELETE FROM rate_limit_counters WHERE window_start < ?", cutoff);
    }
}
