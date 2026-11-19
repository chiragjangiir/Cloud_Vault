package com.cloudvault.unit;

import com.cloudvault.service.BucketLoggingConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketLoggingConfigurationTest {
    @Test
    void testLoggingConfig() {
        BucketLoggingConfiguration cfg = new BucketLoggingConfiguration("logs-bucket", "access-logs/", true);
        assertTrue(cfg.isValid());
        BucketLoggingConfiguration invalid = new BucketLoggingConfiguration("", "prefix", true);
        assertFalse(invalid.isValid());
    }
}
