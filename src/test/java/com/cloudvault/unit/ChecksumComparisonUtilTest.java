package com.cloudvault.unit;

import com.cloudvault.service.ChecksumComparisonUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChecksumComparisonUtilTest {
    @Test
    void testConstantTimeComparison() {
        assertTrue(ChecksumComparisonUtil.constantTimeEquals("abcdef", "abcdef"));
        assertFalse(ChecksumComparisonUtil.constantTimeEquals("abcdef", "xyz123"));
    }
}
