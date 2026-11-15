package com.cloudvault.unit;

import com.cloudvault.service.MemorySizeParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemorySizeParserTest {
    @Test
    void testParseBytes() {
        assertEquals(1024, MemorySizeParser.parseBytes("1KB"));
        assertEquals(5 * 1024 * 1024, MemorySizeParser.parseBytes("5MB"));
        assertEquals(2L * 1024 * 1024 * 1024, MemorySizeParser.parseBytes("2GB"));
        assertEquals(500, MemorySizeParser.parseBytes("500"));
    }
}
