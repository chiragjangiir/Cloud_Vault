package com.cloudvault.unit;

import com.cloudvault.service.S3UploadIdGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3UploadIdGeneratorTest {
    @Test
    void testGenerator() {
        String id = S3UploadIdGenerator.generateId(32);
        assertNotNull(id);
        assertEquals(32, id.length());
    }
}
