package com.cloudvault.unit;

import com.cloudvault.service.ObjectRetentionPeriodValidator;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ObjectRetentionPeriodValidatorTest {
    @Test
    void testRetention() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(3600);
        Instant past = now.minusSeconds(3600);
        assertTrue(ObjectRetentionPeriodValidator.isValidRetention(future, now));
        assertFalse(ObjectRetentionPeriodValidator.isValidRetention(past, now));
    }
}
