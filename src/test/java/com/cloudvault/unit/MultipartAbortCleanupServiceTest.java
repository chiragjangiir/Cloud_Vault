package com.cloudvault.unit;

import com.cloudvault.service.MultipartAbortCleanupService;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class MultipartAbortCleanupServiceTest {
    @Test
    void testUploadExpiration() {
        Instant now = Instant.now();
        Instant old = now.minusSeconds(86400 * 8);
        assertTrue(MultipartAbortCleanupService.isUploadExpired(old, 7, now));
        assertFalse(MultipartAbortCleanupService.isUploadExpired(now.minusSeconds(86400 * 2), 7, now));
    }
}
