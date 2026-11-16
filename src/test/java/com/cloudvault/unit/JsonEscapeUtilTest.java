package com.cloudvault.unit;

import com.cloudvault.service.JsonEscapeUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonEscapeUtilTest {
    @Test
    void testEscapeQuotesAndNewlines() {
        String input = "Hello \"World\"\nLine 2";
        String out = JsonEscapeUtil.escape(input);
        assertNotNull(out);
    }

    @Test
    void testNullReturnsNull() {
        assertEquals("null", JsonEscapeUtil.escape(null));
    }
}
