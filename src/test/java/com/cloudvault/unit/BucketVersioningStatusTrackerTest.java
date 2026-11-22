package com.cloudvault.unit;

import com.cloudvault.service.BucketVersioningStatusTracker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketVersioningStatusTrackerTest {
    @Test
    void testParseStatus() {
        assertEquals(BucketVersioningStatusTracker.VersioningStatus.ENABLED, BucketVersioningStatusTracker.parseStatus("Enabled"));
        assertEquals(BucketVersioningStatusTracker.VersioningStatus.SUSPENDED, BucketVersioningStatusTracker.parseStatus("suspended"));
        assertEquals(BucketVersioningStatusTracker.VersioningStatus.OFF, BucketVersioningStatusTracker.parseStatus(null));
        assertEquals(BucketVersioningStatusTracker.VersioningStatus.OFF, BucketVersioningStatusTracker.parseStatus("invalid"));
    }
}
