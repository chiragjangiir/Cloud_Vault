package com.cloudvault.unit;

import com.cloudvault.service.HttpIfUnmodifiedSinceEvaluator;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class HttpIfUnmodifiedSinceEvaluatorTest {
    @Test
    void testUnmodified() {
        Instant now = Instant.now();
        assertTrue(HttpIfUnmodifiedSinceEvaluator.isUnmodified(now, now.plusSeconds(10)));
        assertFalse(HttpIfUnmodifiedSinceEvaluator.isUnmodified(now.plusSeconds(10), now));
    }
}
