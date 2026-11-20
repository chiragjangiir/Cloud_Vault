package com.cloudvault.unit;

import com.cloudvault.service.SignedQueryParamExtractor;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SignedQueryParamExtractorTest {
    @Test
    void testExtractor() {
        Map<String, String> all = Map.of("X-Amz-Algorithm", "AWS4-HMAC-SHA256", "version", "1");
        Map<String, String> amz = SignedQueryParamExtractor.extractAmzParams(all);
        assertEquals(1, amz.size());
        assertTrue(amz.containsKey("X-Amz-Algorithm"));
    }
}
