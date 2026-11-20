package com.cloudvault.service;

import java.util.zip.CRC32;

public class StreamingCrc32cDigester {
    private final CRC32 crc = new CRC32();

    public void update(byte[] buffer, int offset, int length) {
        crc.update(buffer, offset, length);
    }

    public long getValue() {
        return crc.getValue();
    }
}
