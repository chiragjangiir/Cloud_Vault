package com.cloudvault.unit;

import com.cloudvault.service.ObjectMimeTypeDetector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectMimeTypeDetectorTest {
    @Test
    void testMimeDetection() {
        assertEquals("application/json", ObjectMimeTypeDetector.detectFromFilename("payload.json"));
        assertEquals("image/png", ObjectMimeTypeDetector.detectFromFilename("banner.png"));
        assertEquals("application/octet-stream", ObjectMimeTypeDetector.detectFromFilename("binary_blob"));
    }
}
