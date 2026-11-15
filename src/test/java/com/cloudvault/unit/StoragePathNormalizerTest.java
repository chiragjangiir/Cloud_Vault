package com.cloudvault.unit;

import com.cloudvault.service.StoragePathNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoragePathNormalizerTest {

    @Test
    void testSanitizeStandardPath() {
        assertEquals("docs/report.pdf", StoragePathNormalizer.sanitize("docs/./report.pdf"));
        assertEquals("photos/vacation", StoragePathNormalizer.sanitize("photos/sub/../vacation"));
    }

    @Test
    void testPreventDirectoryTraversal() {
        assertEquals("etc/passwd", StoragePathNormalizer.sanitize("../../etc/passwd"));
        assertTrue(StoragePathNormalizer.containsTraversal("../secret.txt"));
        assertFalse(StoragePathNormalizer.containsTraversal("safe/file.txt"));
    }

    @Test
    void testNullByteStripping() {
        assertTrue(StoragePathNormalizer.containsTraversal("safe.txt\0malicious.exe"));
    }
}
