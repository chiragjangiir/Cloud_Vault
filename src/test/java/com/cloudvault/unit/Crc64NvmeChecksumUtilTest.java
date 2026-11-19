package com.cloudvault.unit;

import com.cloudvault.service.Crc64NvmeChecksumUtil;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class Crc64NvmeChecksumUtilTest {
    @Test
    void testCrc64Checksum() {
        byte[] data = "123456789".getBytes(StandardCharsets.UTF_8);
        long crc = Crc64NvmeChecksumUtil.calculateCrc64(data);
        assertNotEquals(0L, crc);
    }
}
