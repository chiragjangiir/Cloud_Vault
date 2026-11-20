package com.cloudvault.unit;

import com.cloudvault.service.ObjectTagCountLimiter;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ObjectTagCountLimiterTest {
    @Test
    void testTagSetLimiter() {
        Map<String, String> tags = new HashMap<>();
        for (int i = 0; i < 10; i++) tags.put("k" + i, "v" + i);
        assertTrue(ObjectTagCountLimiter.isWithinLimits(tags));
        tags.put("k10", "v10");
        assertFalse(ObjectTagCountLimiter.isWithinLimits(tags));
    }
}
