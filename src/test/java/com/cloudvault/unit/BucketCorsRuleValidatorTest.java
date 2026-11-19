package com.cloudvault.unit;

import com.cloudvault.service.BucketCorsRuleValidator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BucketCorsRuleValidatorTest {
    @Test
    void testCorsRuleValidation() {
        assertTrue(BucketCorsRuleValidator.isValidOrigin("https://example.com"));
        assertTrue(BucketCorsRuleValidator.isValidOrigin("*"));
        assertFalse(BucketCorsRuleValidator.isValidOrigin("ftp://example.com"));
        assertTrue(BucketCorsRuleValidator.isValidMethods(List.of("GET", "PUT")));
    }
}
