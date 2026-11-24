package com.cloudvault.unit;

import com.cloudvault.service.S3RequestDateParser;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class S3RequestDateParserTest {
    @Test
    void testDateParse() {
        Instant inst = S3RequestDateParser.parseIso8601Basic("20261124T230000Z");
        assertNotNull(inst);
        assertNull(S3RequestDateParser.parseIso8601Basic("invalid"));
    }
}
