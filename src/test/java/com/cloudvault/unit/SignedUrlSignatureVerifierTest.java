package com.cloudvault.unit;

import com.cloudvault.service.SignedUrlSignatureVerifier;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SignedUrlSignatureVerifierTest {
    @Test
    void testSignatureVerification() {
        Instant now = Instant.ofEpochSecond(1000);
        assertFalse(SignedUrlSignatureVerifier.isExpired(1050, now));
        assertTrue(SignedUrlSignatureVerifier.isExpired(950, now));
        assertTrue(SignedUrlSignatureVerifier.verifySignature("sig123", "sig123"));
        assertFalse(SignedUrlSignatureVerifier.verifySignature("sig123", "wrong"));
    }
}
