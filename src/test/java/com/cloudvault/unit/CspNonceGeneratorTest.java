package com.cloudvault.unit;

import com.cloudvault.service.CspNonceGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CspNonceGeneratorTest {
    @Test
    void testNonceGeneration() {
        String nonce1 = CspNonceGenerator.generateNonce();
        String nonce2 = CspNonceGenerator.generateNonce();
        assertNotNull(nonce1);
        assertNotEquals(nonce1, nonce2);
        assertTrue(nonce1.length() >= 20);
    }
}
