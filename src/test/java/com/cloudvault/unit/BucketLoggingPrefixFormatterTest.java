package com.cloudvault.unit;

import com.cloudvault.service.BucketLoggingPrefixFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketLoggingPrefixFormatterTest {
    @Test
    void testLoggingPrefix() {
        assertEquals("logs/", BucketLoggingPrefixFormatter.formatLoggingPrefix(null));
        assertEquals("access-logs/", BucketLoggingPrefixFormatter.formatLoggingPrefix("/access-logs"));
    }
}
