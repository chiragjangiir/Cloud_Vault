package com.cloudvault.unit;

import com.cloudvault.service.MultipartPartSizeValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultipartPartSizeValidatorTest {
    @Test
    void testPartSizeValidation() {
        assertTrue(MultipartPartSizeValidator.isValidPartSize(5 * 1024 * 1024, false));
        assertFalse(MultipartPartSizeValidator.isValidPartSize(1024, false));
        assertTrue(MultipartPartSizeValidator.isValidPartSize(1024, true));
        assertFalse(MultipartPartSizeValidator.isValidPartSize(-1, true));
    }
}
