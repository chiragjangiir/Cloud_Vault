package com.cloudvault.unit;

import com.cloudvault.service.S3SelectExpressionValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3SelectExpressionValidatorTest {
    @Test
    void testValidSql() {
        assertTrue(S3SelectExpressionValidator.isValidSql("SELECT * FROM s3object s"));
        assertTrue(S3SelectExpressionValidator.isValidSql("select s._1 from s3object s"));
        assertFalse(S3SelectExpressionValidator.isValidSql("DELETE FROM s3object"));
        assertFalse(S3SelectExpressionValidator.isValidSql(null));
    }
}
