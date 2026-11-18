package com.cloudvault.unit;

import com.cloudvault.service.FileExtensionNormalizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileExtensionNormalizerTest {
    @Test
    void testExtensionNormalization() {
        assertEquals("tar.gz", FileExtensionNormalizer.extractExtension("backup.TAR.GZ"));
        assertEquals("png", FileExtensionNormalizer.extractExtension("image.PNG"));
        assertEquals("", FileExtensionNormalizer.extractExtension("LICENSE"));
        assertTrue(FileExtensionNormalizer.isArchive("data.zip"));
    }
}
