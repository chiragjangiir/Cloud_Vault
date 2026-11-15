package com.cloudvault.unit;

import com.cloudvault.service.SecureCompareUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecureCompareUtilTest {
    @Test
    void testConstantTimeEquals() {
        assertTrue(SecureCompareUtil.constantTimeEquals("token123", "token123"));
        assertFalse(SecureCompareUtil.constantTimeEquals("token123", "wrongtoken"));
        assertTrue(SecureCompareUtil.constantTimeEquals(null, null));
        assertFalse(SecureCompareUtil.constantTimeEquals("a", null));
    }
}
