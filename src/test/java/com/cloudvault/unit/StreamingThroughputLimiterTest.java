package com.cloudvault.unit;

import com.cloudvault.service.StreamingThroughputLimiter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StreamingThroughputLimiterTest {
    @Test
    void testThrottleCalculation() {
        long sleep = StreamingThroughputLimiter.calculateThrottleSleepMillis(1000000L, 500L, 1000000L);
        assertEquals(500L, sleep);

        assertEquals(0L, StreamingThroughputLimiter.calculateThrottleSleepMillis(1000000L, 1200L, 1000000L));
    }
}
