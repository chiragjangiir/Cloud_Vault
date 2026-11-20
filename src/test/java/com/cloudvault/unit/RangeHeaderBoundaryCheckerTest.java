package com.cloudvault.unit;

import com.cloudvault.service.RangeHeaderBoundaryChecker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RangeHeaderBoundaryCheckerTest {
    @Test
    void testRangeCheck() {
        assertTrue(RangeHeaderBoundaryChecker.isSatisfiable(0, 499, 1000));
        assertFalse(RangeHeaderBoundaryChecker.isSatisfiable(1000, 1500, 1000));
    }
}
