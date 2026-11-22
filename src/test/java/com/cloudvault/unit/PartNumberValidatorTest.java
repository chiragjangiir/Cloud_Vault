package com.cloudvault.unit;

import com.cloudvault.service.PartNumberValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartNumberValidatorTest {
    @Test
    void testPartNumber() {
        assertTrue(PartNumberValidator.isValidPartNumber(1));
        assertTrue(PartNumberValidator.isValidPartNumber(10000));
        assertTrue(PartNumberValidator.isValidPartNumber(500));
        assertFalse(PartNumberValidator.isValidPartNumber(0));
        assertFalse(PartNumberValidator.isValidPartNumber(10001));
    }
}
