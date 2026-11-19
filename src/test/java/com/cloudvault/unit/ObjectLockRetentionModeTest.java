package com.cloudvault.unit;

import com.cloudvault.service.ObjectLockRetentionMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectLockRetentionModeTest {
    @Test
    void testRetentionModes() {
        assertEquals(ObjectLockRetentionMode.GOVERNANCE, ObjectLockRetentionMode.fromString("governance"));
        assertEquals(ObjectLockRetentionMode.COMPLIANCE, ObjectLockRetentionMode.fromString("COMPLIANCE"));
        assertNull(ObjectLockRetentionMode.fromString("INVALID"));
    }
}
