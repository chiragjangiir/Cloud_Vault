package com.cloudvault.unit;

import com.cloudvault.service.ByteRateCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ByteRateCalculatorTest {
    @Test
    void testRateCalculations() {
        double rate = ByteRateCalculator.calculateRateBytesPerSec(5 * 1024 * 1024, 1000);
        assertEquals(5.0 * 1024 * 1024, rate, 0.01);
        assertEquals("5.00 MB/s", ByteRateCalculator.formatRate(rate));

        long remaining = ByteRateCalculator.estimateRemainingMillis(10 * 1024 * 1024, rate);
        assertEquals(2000L, remaining);
    }
}
