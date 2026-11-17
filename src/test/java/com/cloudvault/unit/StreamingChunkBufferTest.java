package com.cloudvault.unit;

import com.cloudvault.service.StreamingChunkBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StreamingChunkBufferTest {
    @Test
    void testBufferAccumulates() {
        StreamingChunkBuffer buf = new StreamingChunkBuffer(100);
        byte[] b = new byte[60];
        buf.write(b, 0, 60);
        assertFalse(buf.isReady());
        buf.write(b, 0, 60);
        assertTrue(buf.isReady());
        byte[] flushed = buf.flush();
        assertEquals(120, flushed.length);
        assertEquals(0, buf.size());
    }
}
