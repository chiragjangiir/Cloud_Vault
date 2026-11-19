package com.cloudvault.unit;

import com.cloudvault.service.LifecycleRuleFilter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifecycleRuleFilterTest {
    @Test
    void testRuleFilter() {
        LifecycleRuleFilter filter = new LifecycleRuleFilter("logs/", 500);
        assertTrue(filter.matches("logs/2026.log", 1000));
        assertFalse(filter.matches("photos/img.jpg", 1000));
        assertFalse(filter.matches("logs/empty.log", 100));
    }
}
