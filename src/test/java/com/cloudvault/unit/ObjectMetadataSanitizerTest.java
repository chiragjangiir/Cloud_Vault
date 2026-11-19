package com.cloudvault.unit;

import com.cloudvault.service.ObjectMetadataSanitizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectMetadataSanitizerTest {
    @Test
    void testSanitizer() {
        assertEquals("value with space", ObjectMetadataSanitizer.sanitizeValue("value
with
space"));
        assertEquals("", ObjectMetadataSanitizer.sanitizeValue(null));
    }
}
