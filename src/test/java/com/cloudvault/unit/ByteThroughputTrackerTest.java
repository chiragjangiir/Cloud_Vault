package com.cloudvault.unit;

import com.cloudvault.service.ByteThroughputTracker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ByteThroughputTrackerTest {
    @Test
    void testThroughputTracker() {
        ByteThroughputTracker tracker = new ByteThroughputTracker();
        tracker.recordTransfer(10 * 1024 * 1024, 1000);
        assertEquals(10.0, tracker.getThroughputMegabytesPerSec(), 0.01);
    }
}
