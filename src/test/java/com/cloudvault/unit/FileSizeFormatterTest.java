package com.cloudvault.unit;

import com.cloudvault.service.FileSizeFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FileSizeFormatterTest {
    @Test
    void testFormatBinarySizes() {
        assertEquals("500 B", FileSizeFormatter.formatBinary(500));
        assertEquals("1.00 KiB", FileSizeFormatter.formatBinary(1024));
        assertEquals("5.00 MiB", FileSizeFormatter.formatBinary(5 * 1024 * 1024));
    }
}
