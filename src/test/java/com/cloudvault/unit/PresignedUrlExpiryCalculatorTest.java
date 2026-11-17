package com.cloudvault.unit;

import com.cloudvault.service.PresignedUrlExpiryCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PresignedUrlExpiryCalculatorTest {
    @Test
    void testClampExpiry() {
        assertEquals(60L, PresignedUrlExpiryCalculator.clampExpiry(10L));
        assertEquals(3600L, PresignedUrlExpiryCalculator.clampExpiry(3600L));
        assertEquals(604800L, PresignedUrlExpiryCalculator.clampExpiry(9999999L));
    }
}
