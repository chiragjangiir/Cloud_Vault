package com.cloudvault.unit;

import com.cloudvault.service.StreamingCrc32cDigester;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class StreamingCrc32cDigesterTest {
    @Test
    void testCrc32() {
        StreamingCrc32cDigester digester = new StreamingCrc32cDigester();
        byte[] b = "123456789".getBytes(StandardCharsets.UTF_8);
        digester.update(b, 0, b.length);
        assertEquals(0xCBF43926L, digester.getValue());
    }
}
