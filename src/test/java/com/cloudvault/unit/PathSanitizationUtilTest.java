package com.cloudvault.unit;

import com.cloudvault.service.PathSanitizationUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PathSanitizationUtilTest {
    @Test
    void testCleanPath() {
        assertEquals("a/b", PathSanitizationUtil.cleanPath("a/c/../b"));
    }
}
