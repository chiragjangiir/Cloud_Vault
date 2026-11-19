package com.cloudvault.unit;

import com.cloudvault.service.StorageBandwidthThrottler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageBandwidthThrottlerTest {
    @Test
    void testThrottling() {
        StorageBandwidthThrottler throttler = new StorageBandwidthThrottler(1000);
        assertEquals(1000, throttler.calculateSleepMillis(1000));
        assertEquals(500, throttler.calculateSleepMillis(500));
    }
}
