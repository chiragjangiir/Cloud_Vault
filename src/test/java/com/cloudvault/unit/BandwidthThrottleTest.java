package com.cloudvault.unit;

import com.cloudvault.service.BandwidthThrottle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BandwidthThrottleTest {
    @Test
    void testAcquireWithinCapacity() {
        BandwidthThrottle throttle = new BandwidthThrottle(1024 * 1024);
        assertTrue(throttle.tryAcquire(512 * 1024));
        assertTrue(throttle.tryAcquire(256 * 1024));
    }

    @Test
    void testRejectExceedingBurst() {
        BandwidthThrottle throttle = new BandwidthThrottle(1000);
        assertTrue(throttle.tryAcquire(1000));
        assertFalse(throttle.tryAcquire(500));
    }
}
