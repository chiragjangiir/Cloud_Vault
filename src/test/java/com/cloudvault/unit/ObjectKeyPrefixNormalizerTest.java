package com.cloudvault.unit;

import com.cloudvault.service.ObjectKeyPrefixNormalizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectKeyPrefixNormalizerTest {
    @Test
    void testKeyNormalization() {
        assertEquals("photos/2026/img.png", ObjectKeyPrefixNormalizer.normalize("//photos///2026/img.png"));
        assertEquals("file.txt", ObjectKeyPrefixNormalizer.normalize("/file.txt"));
        assertEquals("", ObjectKeyPrefixNormalizer.normalize(null));
    }
}
