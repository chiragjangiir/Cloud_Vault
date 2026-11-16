package com.cloudvault.unit;

import com.cloudvault.service.MimeTypeDetector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MimeTypeDetectorTest {
    @Test
    void testDetectPng() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        assertEquals("image/png", MimeTypeDetector.detectFromBytes(pngHeader));
    }

    @Test
    void testDetectPdf() {
        byte[] pdfHeader = "%PDF-1.7".getBytes();
        assertEquals("application/pdf", MimeTypeDetector.detectFromBytes(pdfHeader));
    }

    @Test
    void testFallbackOctetStream() {
        byte[] unknown = new byte[]{0x01, 0x02, 0x03};
        assertEquals("application/octet-stream", MimeTypeDetector.detectFromBytes(unknown));
        assertEquals("application/octet-stream", MimeTypeDetector.detectFromBytes(null));
    }
}
