package com.cloudvault.unit;

import com.cloudvault.service.Sha512ChecksumUtil;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class Sha512ChecksumUtilTest {
    @Test
    void testChecksumGeneration() {
        byte[] input = "CloudVaultSecureStorage".getBytes(StandardCharsets.UTF_8);
        String hash = Sha512ChecksumUtil.calculate(input);
        assertNotNull(hash);
        assertEquals(128, hash.length());
        assertTrue(hash.matches("^[0-9a-f]{128}$"));

        assertThrows(IllegalArgumentException.class, () -> Sha512ChecksumUtil.calculate(null));
    }
}
