package com.cloudvault.unit;

import com.cloudvault.service.ChunkedPayloadSigner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChunkedPayloadSignerTest {
    @Test
    void testChunkHeader() {
        String hdr = ChunkedPayloadSigner.formatChunkHeader(1024, "sig123");
        assertEquals("400;chunk-signature=sig123\r\n", hdr);
    }
}
