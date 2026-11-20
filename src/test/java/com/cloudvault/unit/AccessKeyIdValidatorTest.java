package com.cloudvault.unit;

import com.cloudvault.service.AccessKeyIdValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessKeyIdValidatorTest {
    @Test
    void testAccessKey() {
        assertTrue(AccessKeyIdValidator.isValidKey("AKIAIOSFODNN7EXAMPLE"));
        assertTrue(AccessKeyIdValidator.isValidKey("ASIAIOSFODNN7EXAMPLE"));
        assertFalse(AccessKeyIdValidator.isValidKey("SHORT"));
    }
}
