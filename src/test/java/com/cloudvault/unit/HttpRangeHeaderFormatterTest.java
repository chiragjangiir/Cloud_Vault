package com.cloudvault.unit;

import com.cloudvault.service.HttpRangeHeaderFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HttpRangeHeaderFormatterTest {
    @Test
    void testFormatRange() {
        assertEquals("bytes 0-1023/2048", HttpRangeHeaderFormatter.formatContentRange(0, 1023, 2048));
    }
}
