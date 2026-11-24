package com.cloudvault.unit;

import com.cloudvault.service.S3ObjectSizeFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3ObjectSizeFormatterTest {
    @Test
    void testFormatBytes() {
        assertEquals("500 B", S3ObjectSizeFormatter.formatBytes(500));
        assertTrue(S3ObjectSizeFormatter.formatBytes(1048576).contains("MB"));
    }
}
