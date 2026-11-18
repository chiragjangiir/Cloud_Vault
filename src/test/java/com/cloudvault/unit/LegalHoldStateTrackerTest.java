package com.cloudvault.unit;

import com.cloudvault.service.LegalHoldStateTracker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegalHoldStateTrackerTest {
    @Test
    void testHoldPermissions() {
        assertTrue(LegalHoldStateTracker.canDelete(false, false));
        assertFalse(LegalHoldStateTracker.canDelete(true, false));
        assertFalse(LegalHoldStateTracker.canDelete(false, true));
        assertEquals("ON", LegalHoldStateTracker.formatStatus(true));
        assertEquals("OFF", LegalHoldStateTracker.formatStatus(false));
    }
}
