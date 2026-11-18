package com.cloudvault.unit;

import com.cloudvault.service.MimeTypeWhitelistGuard;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MimeTypeWhitelistGuardTest {
    @Test
    void testWhitelist() {
        MimeTypeWhitelistGuard guard = new MimeTypeWhitelistGuard(Set.of("image/*", "application/pdf"));
        assertTrue(guard.isAllowed("image/png"));
        assertTrue(guard.isAllowed("image/jpeg"));
        assertTrue(guard.isAllowed("application/pdf"));
        assertFalse(guard.isAllowed("application/x-sh"));
    }
}
