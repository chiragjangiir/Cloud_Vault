package com.cloudvault.unit;

import com.cloudvault.service.BucketLocationConstraintValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketLocationConstraintValidatorTest {
    @Test
    void testRegion() {
        assertTrue(BucketLocationConstraintValidator.isValidRegion("us-east-1"));
        assertTrue(BucketLocationConstraintValidator.isValidRegion("eu-west-1"));
        assertTrue(BucketLocationConstraintValidator.isValidRegion("ap-south-1"));
        assertFalse(BucketLocationConstraintValidator.isValidRegion("US_EAST_1"));
        assertFalse(BucketLocationConstraintValidator.isValidRegion("invalid"));
    }
}
