package com.cloudvault.unit;

import com.cloudvault.service.FileChecksumUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileChecksumUtilTest {

    @Test
    void testEmptyStreamDigest() throws IOException {
        ByteArrayInputStream empty = new ByteArrayInputStream(new byte[0]);
        String hash = FileChecksumUtil.calculateSha256(empty);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void testStringPayloadDigest() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream("cloudvault".getBytes(StandardCharsets.UTF_8));
        String hash = FileChecksumUtil.calculateSha256(stream);
        assertEquals("9a6b1eadc306883151677c2a25868249d4c045eebf884e5c2819568e750fc3db", hash);
    }

    @Test
    void testNullStreamThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> FileChecksumUtil.calculateSha256(null));
    }
}
