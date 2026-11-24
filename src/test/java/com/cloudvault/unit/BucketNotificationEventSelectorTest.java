package com.cloudvault.unit;

import com.cloudvault.service.BucketNotificationEventSelector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketNotificationEventSelectorTest {
    @Test
    void testNotificationEvent() {
        assertTrue(BucketNotificationEventSelector.matchesEvent("s3:ObjectCreated:*", "s3:ObjectCreated:Put"));
        assertTrue(BucketNotificationEventSelector.matchesEvent("s3:ObjectRemoved:Delete", "s3:ObjectRemoved:Delete"));
        assertFalse(BucketNotificationEventSelector.matchesEvent("s3:ObjectCreated:*", "s3:ObjectRemoved:Delete"));
    }
}
