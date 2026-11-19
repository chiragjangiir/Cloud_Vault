package com.cloudvault.unit;

import com.cloudvault.service.HttpHeaderWhitelistFilter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HttpHeaderWhitelistFilterTest {
    @Test
    void testHeaderWhitelist() {
        assertTrue(HttpHeaderWhitelistFilter.isAllowedHeader("Content-Type"));
        assertTrue(HttpHeaderWhitelistFilter.isAllowedHeader("x-amz-meta-custom-id"));
        assertFalse(HttpHeaderWhitelistFilter.isAllowedHeader("X-Dangerous-Injected-Header"));
    }
}
