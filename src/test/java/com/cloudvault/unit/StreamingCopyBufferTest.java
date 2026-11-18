package com.cloudvault.unit;

import com.cloudvault.service.StreamingCopyBuffer;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class StreamingCopyBufferTest {
    @Test
    void testCopyStream() throws IOException {
        byte[] src = new byte[]{1, 2, 3, 4, 5};
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long copied = StreamingCopyBuffer.copy(in, out, 100);
        assertEquals(5, copied);
        assertArrayEquals(src, out.toByteArray());
    }

    @Test
    void testCopyExceedsLimit() {
        byte[] src = new byte[]{1, 2, 3, 4, 5};
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(IOException.class, () -> StreamingCopyBuffer.copy(in, out, 3));
    }
}
