package com.cloudvault.unit;

import com.cloudvault.service.BucketPolicyStatementValidator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BucketPolicyStatementValidatorTest {
    @Test
    void testPolicyValidation() {
        BucketPolicyStatementValidator stmt = new BucketPolicyStatementValidator("Allow", List.of("s3:GetObject"), "arn:aws:s3:::mybucket/*");
        assertTrue(stmt.isValid());
        BucketPolicyStatementValidator invalid = new BucketPolicyStatementValidator("Maybe", List.of(), "invalid");
        assertFalse(invalid.isValid());
    }
}
