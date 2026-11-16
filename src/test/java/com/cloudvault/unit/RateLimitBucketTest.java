package com.cloudvault.unit;

import com.cloudvault.service.RateLimitBucket;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RateLimitBucketTest {
    @Test
    void testConsumeTokens() {
        RateLimitBucket bucket = new RateLimitBucket(10);
        assertTrue(bucket.consume(5));
        assertEquals(5, bucket.getTokens());
        assertFalse(bucket.consume(6));
        bucket.refill(5);
        assertEquals(10, bucket.getTokens());
    }
}
