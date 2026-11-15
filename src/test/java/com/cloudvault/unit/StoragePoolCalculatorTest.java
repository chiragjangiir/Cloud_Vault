package com.cloudvault.unit;

import com.cloudvault.service.StoragePoolCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StoragePoolCalculatorTest {
    @Test
    void testCalculateAvailableBytes() {
        assertEquals(700, StoragePoolCalculator.calculateAvailableBytes(1000, 200, 100));
        assertEquals(0, StoragePoolCalculator.calculateAvailableBytes(1000, 950, 100));
    }
}
