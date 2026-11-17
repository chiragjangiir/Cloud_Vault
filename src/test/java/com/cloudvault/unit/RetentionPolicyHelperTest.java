package com.cloudvault.unit;

import com.cloudvault.service.RetentionPolicyHelper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.*;

public class RetentionPolicyHelperTest {
    @Test
    void testLegalHoldOverridesDate() {
        assertTrue(RetentionPolicyHelper.isRetained(null, true));
    }

    @Test
    void testFutureDateRetained() {
        Instant future = Instant.now().plus(10, ChronoUnit.DAYS);
        assertTrue(RetentionPolicyHelper.isRetained(future, false));
    }

    @Test
    void testPastDateNotRetained() {
        Instant past = Instant.now().minus(10, ChronoUnit.DAYS);
        assertFalse(RetentionPolicyHelper.isRetained(past, false));
    }
}
