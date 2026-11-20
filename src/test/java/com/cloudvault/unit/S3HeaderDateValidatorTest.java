package com.cloudvault.unit;

import com.cloudvault.service.S3HeaderDateValidator;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class S3HeaderDateValidatorTest {
    @Test
    void testClockSkew() {
        Instant server = Instant.now();
        assertTrue(S3HeaderDateValidator.isClockSkewAcceptable(server.minusSeconds(300), server));
        assertFalse(S3HeaderDateValidator.isClockSkewAcceptable(server.minusSeconds(1200), server));
    }
}
