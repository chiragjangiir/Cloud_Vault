package com.cloudvault.unit;

import com.cloudvault.service.CrossRegionReplicationConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrossRegionReplicationConfigTest {
    @Test
    void testReplicationConfig() {
        CrossRegionReplicationConfig cfg = new CrossRegionReplicationConfig("arn:aws:iam::1:role/r", "arn:aws:s3:::b", "STANDARD");
        assertTrue(cfg.isValid());
    }
}
