package com.cloudvault.unit;

import com.cloudvault.service.ConcurrentTransferLimiter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentTransferLimiterTest {
    @Test
    void testLimitTransfers() {
        ConcurrentTransferLimiter limiter = new ConcurrentTransferLimiter(2);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        limiter.release();
        assertTrue(limiter.tryAcquire());
    }
}
