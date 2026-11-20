package com.cloudvault.unit;

import com.cloudvault.service.BucketLifecycleExpirationCalculator;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class BucketLifecycleExpirationCalculatorTest {
    @Test
    void testExpiryCalculation() {
        Instant now = Instant.ofEpochSecond(1000);
        Instant exp = BucketLifecycleExpirationCalculator.computeExpiry(now, 1);
        assertEquals(now.plusSeconds(86400), exp);
    }
}
