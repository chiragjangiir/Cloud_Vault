package com.cloudvault.unit;

import com.cloudvault.service.FileValidationUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationUtilTest {

    @Test
    void testExtractExtension() {
        assertEquals("pdf", FileValidationUtil.extractExtension("report.pdf"));
        assertEquals("tar.gz".substring(4), FileValidationUtil.extractExtension("archive.tar.gz"));
        assertEquals("", FileValidationUtil.extractExtension("Makefile"));
        assertEquals("", FileValidationUtil.extractExtension(".hidden"));
    }

    @Test
    void testDangerousExecutables() {
        assertTrue(FileValidationUtil.isDangerousExecutable("malware.exe"));
        assertTrue(FileValidationUtil.isDangerousExecutable("script.SH"));
        assertTrue(FileValidationUtil.isDangerousExecutable("payload.bat"));
        assertFalse(FileValidationUtil.isDangerousExecutable("document.docx"));
        assertFalse(FileValidationUtil.isDangerousExecutable("image.png"));
    }

    @Test
    void testAllowedFilename() {
        assertTrue(FileValidationUtil.isAllowedFilename("annual_report_2026.pdf"));
        assertFalse(FileValidationUtil.isAllowedFilename("../etc/passwd"));
        assertFalse(FileValidationUtil.isAllowedFilename("sub/path/file.txt"));
        assertFalse(FileValidationUtil.isAllowedFilename("trojan.exe"));
        assertFalse(FileValidationUtil.isAllowedFilename("   "));
    }
}
