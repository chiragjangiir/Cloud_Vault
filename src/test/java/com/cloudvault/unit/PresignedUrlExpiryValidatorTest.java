package com.cloudvault.unit;

import com.cloudvault.service.PresignedUrlExpiryValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresignedUrlExpiryValidatorTest {
    @Test
    void testExpiryLimits() {
        assertTrue(PresignedUrlExpiryValidator.isWithinAllowedMax(3600));
        assertTrue(PresignedUrlExpiryValidator.isWithinAllowedMax(604800));
        assertFalse(PresignedUrlExpiryValidator.isWithinAllowedMax(604801));
        assertFalse(PresignedUrlExpiryValidator.isWithinAllowedMax(-1));
    }
}
