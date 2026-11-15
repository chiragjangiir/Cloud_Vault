package com.cloudvault.unit;

import com.cloudvault.service.ByteRangeParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteRangeParserTest {
    @Test
    void testByteRangeParsing() {
        ByteRangeParser.ByteRange range = ByteRangeParser.parse("bytes=0-499", 1000);
        assertEquals(0, range.start());
        assertEquals(499, range.end());

        ByteRangeParser.ByteRange rangeSuffix = ByteRangeParser.parse("bytes=500-", 1000);
        assertEquals(500, rangeSuffix.start());
        assertEquals(999, rangeSuffix.end());
    }
}
