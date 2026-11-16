package com.cloudvault.unit;

import com.cloudvault.service.StreamBufferPool;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StreamBufferPoolTest {
    @Test
    void testBufferAcquireAndRelease() {
        StreamBufferPool pool = new StreamBufferPool(64 * 1024);
        byte[] b1 = pool.acquire();
        assertEquals(64 * 1024, b1.length);
        assertEquals(0, pool.getPoolSize());
        pool.release(b1);
        assertEquals(1, pool.getPoolSize());
        byte[] b2 = pool.acquire();
        assertSame(b1, b2);
    }
}
