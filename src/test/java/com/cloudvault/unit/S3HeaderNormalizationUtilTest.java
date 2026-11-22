package com.cloudvault.unit;

import com.cloudvault.service.S3HeaderNormalizationUtil;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class S3HeaderNormalizationUtilTest {
    @Test
    void testNormalizeHeaders() {
        Map<String, String> raw = Map.of(
            "X-Amz-Storage-Class", " STANDARD ",
            "Content-Type", "application/json"
        );
        Map<String, String> res = S3HeaderNormalizationUtil.normalizeHeaders(raw);
        assertEquals("STANDARD", res.get("x-amz-storage-class"));
        assertEquals("application/json", res.get("Content-Type"));
    }
}
