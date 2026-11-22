package com.cloudvault.unit;

import com.cloudvault.service.PresignedUrlSignatureMatcher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresignedUrlSignatureMatcherTest {
    @Test
    void testSignatureMatching() {
        assertTrue(PresignedUrlSignatureMatcher.constantTimeEquals("abcdef123456", "abcdef123456"));
        assertFalse(PresignedUrlSignatureMatcher.constantTimeEquals("abcdef123456", "abcdef123457"));
        assertFalse(PresignedUrlSignatureMatcher.constantTimeEquals(null, "abcdef123456"));
    }
}
