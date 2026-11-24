package com.cloudvault.unit;

import com.cloudvault.service.ObjectMetadataValidator;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ObjectMetadataValidatorTest {
    @Test
    void testMetadataSize() {
        assertTrue(ObjectMetadataValidator.isWithinSizeLimit(Map.of("author", "john", "version", "1")));
        assertFalse(ObjectMetadataValidator.isWithinSizeLimit(Map.of("data", "x".repeat(2050))));
    }
}
