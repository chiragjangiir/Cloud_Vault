package com.cloudvault.unit;

import com.cloudvault.service.ChunkSizeCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkSizeCalculatorTest {
    @Test
    void testSmallFileUsesMinimumChunk() {
        assertEquals(5 * 1024 * 1024L, ChunkSizeCalculator.calculateChunkSize(1024L));
        assertEquals(1, ChunkSizeCalculator.calculatePartCount(1024L, 5 * 1024 * 1024L));
    }

    @Test
    void testHugeFileScalesChunkSize() {
        long fiftyGigabytes = 50L * 1024 * 1024 * 1024;
        long chunk = ChunkSizeCalculator.calculateChunkSize(fiftyGigabytes);
        assertTrue(chunk >= ChunkSizeCalculator.MIN_CHUNK_SIZE);
        assertTrue(ChunkSizeCalculator.calculatePartCount(fiftyGigabytes, chunk) <= 10000);
    }

    @Test
    void testPartCountZeroWhenEmpty() {
        assertEquals(0, ChunkSizeCalculator.calculatePartCount(0, 1024));
    }
}
