package com.cloudvault.unit;

import com.cloudvault.service.HttpContentTypeNormalizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HttpContentTypeNormalizerTest {
    @Test
    void testContentTypeNormalization() {
        assertEquals("application/json", HttpContentTypeNormalizer.normalize("application/json; charset=utf-8"));
        assertEquals("text/html", HttpContentTypeNormalizer.normalize("  TEXT/HTML  "));
    }
}
