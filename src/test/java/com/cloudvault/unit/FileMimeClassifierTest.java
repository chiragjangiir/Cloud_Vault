package com.cloudvault.unit;

import com.cloudvault.service.FileMimeClassifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileMimeClassifierTest {
    @Test
    void testMimeClassification() {
        assertEquals("application/json", FileMimeClassifier.classify("data.json"));
        assertEquals("image/png", FileMimeClassifier.classify("photo.PNG"));
        assertEquals("application/octet-stream", FileMimeClassifier.classify("bin.unknown"));
    }
}
