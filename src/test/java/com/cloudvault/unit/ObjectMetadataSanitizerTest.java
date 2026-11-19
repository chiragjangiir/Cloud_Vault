package com.cloudvault.unit;

import com.cloudvault.service.ObjectMetadataSanitizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectMetadataSanitizerTest {
    @Test
    void testSanitizer() {
        String input = "value" + "\r\n" + "with" + "\n" + "space";
        assertEquals("value with space", ObjectMetadataSanitizer.sanitizeValue(input));
        assertEquals("", ObjectMetadataSanitizer.sanitizeValue(null));
    }
}
