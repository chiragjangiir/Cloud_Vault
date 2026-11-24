package com.cloudvault.unit;

import com.cloudvault.service.RangeHeaderByteParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RangeHeaderByteParserTest {
    @Test
    void testRangeParser() {
        RangeHeaderByteParser r = RangeHeaderByteParser.parse("bytes=0-499", 1000);
        assertNotNull(r);
        assertEquals(0, r.getStart());
        assertEquals(499, r.getEnd());
        assertEquals(500, r.getLength());
    }
}
