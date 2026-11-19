package com.cloudvault.unit;

import com.cloudvault.service.Md5StreamingDigestUtil;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class Md5StreamingDigestUtilTest {
    @Test
    void testMd5Streaming() throws IOException {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        String hex = Md5StreamingDigestUtil.computeHexDigest(new ByteArrayInputStream(data));
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", hex);
    }
}
