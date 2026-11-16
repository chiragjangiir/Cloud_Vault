package com.cloudvault.unit;

import com.cloudvault.service.CryptoRandomUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CryptoRandomUtilTest {
    @Test
    void testGenerateHex() {
        String hex = CryptoRandomUtil.generateRandomHex(16);
        assertEquals(32, hex.length());
    }
}
