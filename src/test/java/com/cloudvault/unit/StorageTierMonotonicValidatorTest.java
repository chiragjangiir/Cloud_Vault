package com.cloudvault.unit;

import com.cloudvault.service.StorageTierMonotonicValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageTierMonotonicValidatorTest {
    @Test
    void testTransitions() {
        assertTrue(StorageTierMonotonicValidator.isTransitionPermitted("HOT", "WARM"));
        assertTrue(StorageTierMonotonicValidator.isTransitionPermitted("WARM", "ARCHIVE"));
        assertFalse(StorageTierMonotonicValidator.isTransitionPermitted("ARCHIVE", "HOT"));
    }
}
