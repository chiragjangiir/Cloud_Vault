package com.cloudvault.unit;

import com.cloudvault.service.HttpIfMatchHeaderEvaluator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HttpIfMatchHeaderEvaluatorTest {
    @Test
    void testIfMatch() {
        assertTrue(HttpIfMatchHeaderEvaluator.matches("*", "\"etag123\""));
        assertTrue(HttpIfMatchHeaderEvaluator.matches("\"etag123\"", "etag123"));
        assertTrue(HttpIfMatchHeaderEvaluator.matches("etag1, etag2, etag3", "etag2"));
        assertFalse(HttpIfMatchHeaderEvaluator.matches("etag1", "etag2"));
    }
}
