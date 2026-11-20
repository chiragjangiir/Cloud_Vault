package com.cloudvault.unit;

import com.cloudvault.service.ObjectKeyLengthValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectKeyLengthValidatorTest {
    @Test
    void testKeyLength() {
        assertTrue(ObjectKeyLengthValidator.isValidKeyLength("folder/file.txt"));
        assertFalse(ObjectKeyLengthValidator.isValidKeyLength(""));
        assertFalse(ObjectKeyLengthValidator.isValidKeyLength(null));
    }
}
