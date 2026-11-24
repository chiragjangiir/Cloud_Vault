package com.cloudvault.unit;

import com.cloudvault.service.BucketPolicyPrincipalValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketPolicyPrincipalValidatorTest {
    @Test
    void testPrincipal() {
        assertTrue(BucketPolicyPrincipalValidator.isValidPrincipal("*"));
        assertTrue(BucketPolicyPrincipalValidator.isValidPrincipal("arn:aws:iam::123456789012:root"));
        assertFalse(BucketPolicyPrincipalValidator.isValidPrincipal("user:admin"));
    }
}
