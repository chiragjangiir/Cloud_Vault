package com.cloudvault.unit;

import com.cloudvault.service.BucketNameValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketNameValidatorTest {
    @Test
    void testBucketValidation() {
        assertTrue(BucketNameValidator.isValid("my-cloud-bucket-01"));
        assertFalse(BucketNameValidator.isValid("MyBucket"));
        assertFalse(BucketNameValidator.isValid("ab"));
        assertFalse(BucketNameValidator.isValid("192.168.1.1"));
        assertFalse(BucketNameValidator.isValid("-invalid-bucket"));
    }
}
