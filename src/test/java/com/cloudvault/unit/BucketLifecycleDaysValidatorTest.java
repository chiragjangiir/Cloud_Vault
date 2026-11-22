package com.cloudvault.unit;

import com.cloudvault.service.BucketLifecycleDaysValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketLifecycleDaysValidatorTest {
    @Test
    void testLifecycleDays() {
        assertTrue(BucketLifecycleDaysValidator.isValidLifecycleTransition(30, 90));
        assertFalse(BucketLifecycleDaysValidator.isValidLifecycleTransition(90, 30));
        assertFalse(BucketLifecycleDaysValidator.isValidLifecycleTransition(30, 30));
        assertFalse(BucketLifecycleDaysValidator.isValidLifecycleTransition(-1, 90));
    }
}
