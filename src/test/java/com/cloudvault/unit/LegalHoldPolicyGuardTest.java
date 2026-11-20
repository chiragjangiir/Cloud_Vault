package com.cloudvault.unit;

import com.cloudvault.service.LegalHoldPolicyGuard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegalHoldPolicyGuardTest {
    @Test
    void testPolicyGuard() {
        assertTrue(LegalHoldPolicyGuard.isDeletionAllowed(false, false));
        assertFalse(LegalHoldPolicyGuard.isDeletionAllowed(true, false));
        assertFalse(LegalHoldPolicyGuard.isDeletionAllowed(false, true));
    }
}
