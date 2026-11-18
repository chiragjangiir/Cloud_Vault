package com.cloudvault.unit;

import com.cloudvault.service.ObjectTaggingValidator;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ObjectTaggingValidatorTest {
    @Test
    void testValidTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("environment", "production");
        tags.put("cost_center", "4921");
        assertDoesNotThrow(() -> ObjectTaggingValidator.validate(tags));
    }

    @Test
    void testInvalidTagKey() {
        assertFalse(ObjectTaggingValidator.isValidKey(""));
        assertFalse(ObjectTaggingValidator.isValidKey(null));
        assertTrue(ObjectTaggingValidator.isValidValue(""));
    }
}
