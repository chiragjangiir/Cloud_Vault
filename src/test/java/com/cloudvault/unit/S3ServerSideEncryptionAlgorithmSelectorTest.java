package com.cloudvault.unit;

import com.cloudvault.service.S3ServerSideEncryptionAlgorithmSelector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3ServerSideEncryptionAlgorithmSelectorTest {
    @Test
    void testSseSelector() {
        assertEquals(S3ServerSideEncryptionAlgorithmSelector.Algorithm.AES256, S3ServerSideEncryptionAlgorithmSelector.parse("AES256"));
        assertEquals(S3ServerSideEncryptionAlgorithmSelector.Algorithm.AWS_KMS, S3ServerSideEncryptionAlgorithmSelector.parse("aws:kms"));
        assertEquals(S3ServerSideEncryptionAlgorithmSelector.Algorithm.NONE, S3ServerSideEncryptionAlgorithmSelector.parse(null));
    }
}
