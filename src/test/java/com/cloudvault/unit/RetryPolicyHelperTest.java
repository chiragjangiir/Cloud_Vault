package com.cloudvault.unit;

import com.cloudvault.service.RetryPolicyHelper;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class RetryPolicyHelperTest {
    @Test
    void testExponentialGrowth() {
        long d1 = RetryPolicyHelper.calculateBackoff(1, 100, 10000, null);
        long d2 = RetryPolicyHelper.calculateBackoff(2, 100, 10000, null);
        long d3 = RetryPolicyHelper.calculateBackoff(3, 100, 10000, null);
        assertEquals(100, d1);
        assertEquals(200, d2);
        assertEquals(400, d3);
    }

    @Test
    void testCapMaxDelay() {
        long d10 = RetryPolicyHelper.calculateBackoff(10, 1000, 5000, null);
        assertEquals(5000, d10);
    }

    @Test
    void testShouldRetryOnIoException() {
        assertTrue(RetryPolicyHelper.shouldRetry(1, 3, new IOException("Timeout")));
        assertFalse(RetryPolicyHelper.shouldRetry(3, 3, new IOException("Timeout")));
        assertFalse(RetryPolicyHelper.shouldRetry(1, 3, new IllegalArgumentException()));
    }
}
