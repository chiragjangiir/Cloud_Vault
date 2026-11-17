package com.cloudvault.unit;

import com.cloudvault.service.ObjectMetadataValidator;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ObjectMetadataValidatorTest {
    @Test
    void testValidMetadata() {
        assertTrue(ObjectMetadataValidator.isValidMetadata(Map.of("project", "cloudvault", "author", "chirag")));
    }

    @Test
    void testRejectEmptyKey() {
        assertFalse(ObjectMetadataValidator.isValidMetadata(Map.of("", "val")));
    }
}
