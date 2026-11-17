package com.cloudvault.unit;

import com.cloudvault.service.UploadSessionTracker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UploadSessionTrackerTest {
    @Test
    void testSessionLifecycle() {
        UploadSessionTracker tracker = new UploadSessionTracker();
        tracker.startSession("sess-1");
        assertTrue(tracker.isActive("sess-1"));
        assertEquals(1, tracker.getActiveSessionCount());
        tracker.endSession("sess-1");
        assertFalse(tracker.isActive("sess-1"));
    }
}
