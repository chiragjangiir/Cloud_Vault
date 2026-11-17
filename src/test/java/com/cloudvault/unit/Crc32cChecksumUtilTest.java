package com.cloudvault.unit;

import com.cloudvault.service.Crc32cChecksumUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Crc32cChecksumUtilTest {
    @Test
    void testCrc32Calculation() {
        byte[] bytes = "CloudVault".getBytes();
        long crc = Crc32cChecksumUtil.calculateCrc32(bytes);
        assertTrue(crc > 0);
    }
}
