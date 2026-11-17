package com.cloudvault.service;

import java.util.zip.CRC32;

public class Crc32cChecksumUtil {
    public static long calculateCrc32(byte[] data) {
        if (data == null) return 0L;
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
}
