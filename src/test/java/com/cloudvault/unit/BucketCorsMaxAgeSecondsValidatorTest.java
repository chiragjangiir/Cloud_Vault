package com.cloudvault.unit;

import com.cloudvault.service.BucketCorsMaxAgeSecondsValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketCorsMaxAgeSecondsValidatorTest {
    @Test
    void testCorsAge() {
        assertTrue(BucketCorsMaxAgeSecondsValidator.isValidMaxAge(3600));
        assertTrue(BucketCorsMaxAgeSecondsValidator.isValidMaxAge(0));
        assertFalse(BucketCorsMaxAgeSecondsValidator.isValidMaxAge(-1));
        assertFalse(BucketCorsMaxAgeSecondsValidator.isValidMaxAge(90000));
    }
}
