package com.cloudvault.unit;

import com.cloudvault.service.ObjectLockLegalHoldValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectLockLegalHoldValidatorTest {
    @Test
    void testLegalHold() {
        assertTrue(ObjectLockLegalHoldValidator.isValidStatus("ON"));
        assertTrue(ObjectLockLegalHoldValidator.isValidStatus("off"));
        assertFalse(ObjectLockLegalHoldValidator.isValidStatus("DISABLED"));
        assertEquals(ObjectLockLegalHoldValidator.LegalHoldStatus.ON, ObjectLockLegalHoldValidator.parseStatus("ON"));
    }
}
