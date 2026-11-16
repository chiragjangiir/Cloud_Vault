package com.cloudvault.unit;

import com.cloudvault.service.StorageKeyGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StorageKeyGeneratorTest {
    @Test
    void testGenerateKey() {
        String key = StorageKeyGenerator.generateKey("tenant-42", "document.pdf");
        assertTrue(key.startsWith("tenant-42/"));
        assertTrue(key.endsWith(".pdf"));
    }
}
