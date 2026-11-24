package com.cloudvault.unit;

import com.cloudvault.service.S3InventoryFrequencyValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3InventoryFrequencyValidatorTest {
    @Test
    void testFrequency() {
        assertEquals(S3InventoryFrequencyValidator.Frequency.DAILY, S3InventoryFrequencyValidator.parse("Daily"));
        assertEquals(S3InventoryFrequencyValidator.Frequency.WEEKLY, S3InventoryFrequencyValidator.parse("WEEKLY"));
        assertNull(S3InventoryFrequencyValidator.parse("MONTHLY"));
    }
}
