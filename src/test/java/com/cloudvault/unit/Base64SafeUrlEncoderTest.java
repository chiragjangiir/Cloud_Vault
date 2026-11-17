package com.cloudvault.unit;

import com.cloudvault.service.Base64SafeUrlEncoder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Base64SafeUrlEncoderTest {
    @Test
    void testUrlSafeBase64() {
        byte[] original = "cloudvault-cursor-token-2026".getBytes();
        String encoded = Base64SafeUrlEncoder.encode(original);
        assertFalse(encoded.contains("="));
        assertArrayEquals(original, Base64SafeUrlEncoder.decode(encoded));
    }
}
