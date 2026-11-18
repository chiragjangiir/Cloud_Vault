package com.cloudvault.unit;

import com.cloudvault.service.StorageQuotaEnforcer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageQuotaEnforcerTest {
    @Test
    void testQuotaLimits() {
        assertTrue(StorageQuotaEnforcer.canFit(100, 50, 200));
        assertTrue(StorageQuotaEnforcer.canFit(100, 100, 200));
        assertFalse(StorageQuotaEnforcer.canFit(100, 101, 200));
        assertFalse(StorageQuotaEnforcer.canFit(-1, 50, 200));

        assertDoesNotThrow(() -> StorageQuotaEnforcer.checkQuota(100, 50, 200));
        assertThrows(IllegalStateException.class, () -> StorageQuotaEnforcer.checkQuota(100, 101, 200));
    }
}
