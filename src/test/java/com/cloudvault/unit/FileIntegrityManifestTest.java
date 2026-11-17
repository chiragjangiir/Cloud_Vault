package com.cloudvault.unit;

import com.cloudvault.service.FileIntegrityManifest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FileIntegrityManifestTest {
    @Test
    void testManifestCreation() {
        FileIntegrityManifest m = new FileIntegrityManifest("sha256hex", "md5hex", 1024L);
        assertEquals("sha256hex", m.getSha256());
        assertEquals("md5hex", m.getMd5());
        assertEquals(1024L, m.getSizeBytes());
    }
}
