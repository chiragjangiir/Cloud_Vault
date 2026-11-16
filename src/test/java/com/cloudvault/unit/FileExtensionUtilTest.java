package com.cloudvault.unit;

import com.cloudvault.service.FileExtensionUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FileExtensionUtilTest {
    @Test
    void testExtractExtension() {
        assertEquals("png", FileExtensionUtil.getExtension("profile.PNG"));
        assertEquals("gz", FileExtensionUtil.getExtension("/path/to/archive.tar.gz"));
        assertEquals("", FileExtensionUtil.getExtension("noextension"));
        assertEquals("", FileExtensionUtil.getExtension(".hidden"));
    }

    @Test
    void testExtensionClassification() {
        assertTrue(FileExtensionUtil.isArchive("backup.zip"));
        assertTrue(FileExtensionUtil.isImage("photo.jpeg"));
        assertFalse(FileExtensionUtil.isImage("document.pdf"));
    }
}
