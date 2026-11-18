package com.cloudvault.unit;

import com.cloudvault.service.HttpDateHeaderParser;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class HttpDateHeaderParserTest {
    @Test
    void testHttpDate() {
        Instant now = Instant.ofEpochSecond(1700000000);
        String formatted = HttpDateHeaderParser.format(now);
        assertNotNull(formatted);
        Instant parsed = HttpDateHeaderParser.parse(formatted);
        assertEquals(now, parsed);
    }
}
