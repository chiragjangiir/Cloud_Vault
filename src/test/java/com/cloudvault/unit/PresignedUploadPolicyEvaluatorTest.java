package com.cloudvault.unit;

import com.cloudvault.service.PresignedUploadPolicyEvaluator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresignedUploadPolicyEvaluatorTest {
    @Test
    void testPolicyEvaluation() {
        assertTrue(PresignedUploadPolicyEvaluator.isContentLengthAllowed(1024, 100, 5000));
        assertFalse(PresignedUploadPolicyEvaluator.isContentLengthAllowed(50, 100, 5000));
    }
}
