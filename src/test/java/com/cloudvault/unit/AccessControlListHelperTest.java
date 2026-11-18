package com.cloudvault.unit;

import com.cloudvault.service.AccessControlListHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessControlListHelperTest {
    @Test
    void testAclValidation() {
        assertTrue(AccessControlListHelper.isValidCannedAcl("public-read"));
        assertTrue(AccessControlListHelper.isValidCannedAcl("PRIVATE"));
        assertFalse(AccessControlListHelper.isValidCannedAcl("root-access"));
        assertEquals("public-read", AccessControlListHelper.normalizeAcl("  PUBLIC-READ  "));
        assertEquals("private", AccessControlListHelper.normalizeAcl("invalid"));
    }
}
