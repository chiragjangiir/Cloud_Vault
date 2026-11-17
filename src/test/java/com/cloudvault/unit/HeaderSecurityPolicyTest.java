package com.cloudvault.unit;

import com.cloudvault.service.HeaderSecurityPolicy;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class HeaderSecurityPolicyTest {
    @Test
    void testSecurityHeadersPresent() {
        Map<String, String> headers = HeaderSecurityPolicy.getDownloadHeaders();
        assertEquals("nosniff", headers.get("X-Content-Type-Options"));
        assertEquals("DENY", headers.get("X-Frame-Options"));
    }
}
