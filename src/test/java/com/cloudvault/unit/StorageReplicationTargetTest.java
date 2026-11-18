package com.cloudvault.unit;

import com.cloudvault.service.StorageReplicationTarget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageReplicationTargetTest {
    @Test
    void testReplicationTarget() {
        StorageReplicationTarget target = new StorageReplicationTarget("backup-us-east-1", "us-east-1", true);
        assertTrue(target.isValid());
        assertEquals("arn:aws:s3:::backup-us-east-1", target.toArn());
    }
}
