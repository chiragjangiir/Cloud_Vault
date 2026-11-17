package com.cloudvault.unit;

import com.cloudvault.service.FilenameSanitizerUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilenameSanitizerUtilTest {
    @Test
    void testSanitizeReservedNames() {
        assertEquals("safe_CON.txt", FilenameSanitizerUtil.sanitize("CON.txt"));
        assertEquals("safe_nul", FilenameSanitizerUtil.sanitize("nul"));
    }

    @Test
    void testSanitizeIllegalChars() {
        assertEquals("my_report_2026.pdf", FilenameSanitizerUtil.sanitize("my:report*2026.pdf"));
    }
}
