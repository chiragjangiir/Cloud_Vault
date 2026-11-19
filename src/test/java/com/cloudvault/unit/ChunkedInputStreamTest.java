package com.cloudvault.unit;

import com.cloudvault.service.ChunkedInputStream;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class ChunkedInputStreamTest {
    @Test
    void testChunkedRead() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
        ChunkedInputStream cis = new ChunkedInputStream(bais, 3);
        assertEquals(1, cis.read());
        assertEquals(2, cis.read());
        assertEquals(3, cis.read());
        assertEquals(-1, cis.read());
    }
}
