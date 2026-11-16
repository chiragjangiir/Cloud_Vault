package com.cloudvault.unit;

import com.cloudvault.service.DigestAlgorithmUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DigestAlgorithmUtilTest {
    @Test
    void testSha256Digest() {
        byte[] digest = DigestAlgorithmUtil.computeDigest("SHA-256", "hello".getBytes());
        assertNotNull(digest);
        assertEquals(32, digest.length);
    }

    @Test
    void testSupportedAlgorithm() {
        assertTrue(DigestAlgorithmUtil.isSupportedAlgorithm("SHA-256"));
        assertTrue(DigestAlgorithmUtil.isSupportedAlgorithm("SHA-512"));
        assertFalse(DigestAlgorithmUtil.isSupportedAlgorithm("UNKNOWN-DIGEST-99"));
    }
}
