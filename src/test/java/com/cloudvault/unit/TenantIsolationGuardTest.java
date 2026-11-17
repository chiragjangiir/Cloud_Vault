package com.cloudvault.unit;

import com.cloudvault.service.TenantIsolationGuard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TenantIsolationGuardTest {
    @Test
    void testTenantPrefixEnforcement() {
        assertEquals("tenantA/doc.pdf", TenantIsolationGuard.enforceTenantPrefix("doc.pdf", "tenantA"));
        assertEquals("tenantA/doc.pdf", TenantIsolationGuard.enforceTenantPrefix("tenantA/doc.pdf", "tenantA"));
    }
}
