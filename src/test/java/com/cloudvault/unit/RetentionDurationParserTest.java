package com.cloudvault.unit;

import com.cloudvault.service.RetentionDurationParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetentionDurationParserTest {
    @Test
    void testDurationParsing() {
        assertEquals(30, RetentionDurationParser.parseDays("P30D"));
        assertEquals(365, RetentionDurationParser.parseDays("P1Y"));
        assertEquals(0, RetentionDurationParser.parseDays("INVALID"));
    }
}
