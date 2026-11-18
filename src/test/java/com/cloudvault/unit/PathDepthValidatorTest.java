package com.cloudvault.unit;

import com.cloudvault.service.PathDepthValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PathDepthValidatorTest {
    @Test
    void testDepthCalculation() {
        assertEquals(0, PathDepthValidator.calculateDepth(""));
        assertEquals(1, PathDepthValidator.calculateDepth("photos/"));
        assertEquals(4, PathDepthValidator.calculateDepth("photos/2026/nov/photo.jpg"));
        assertTrue(PathDepthValidator.isWithinMaxDepth("a/b/c", 5));
        assertFalse(PathDepthValidator.isWithinMaxDepth("a/b/c", 2));
    }
}
