package com.cloudvault.unit;

import com.cloudvault.service.ObjectLockModeSelector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectLockModeSelectorTest {
    @Test
    void testLockMode() {
        assertEquals(ObjectLockModeSelector.Mode.GOVERNANCE, ObjectLockModeSelector.parse("Governance"));
        assertEquals(ObjectLockModeSelector.Mode.COMPLIANCE, ObjectLockModeSelector.parse("compliance"));
        assertEquals(ObjectLockModeSelector.Mode.NONE, ObjectLockModeSelector.parse(null));
    }
}
