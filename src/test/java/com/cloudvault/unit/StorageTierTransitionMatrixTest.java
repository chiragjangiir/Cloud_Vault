package com.cloudvault.unit;

import com.cloudvault.service.StorageTierTransitionMatrix;
import com.cloudvault.service.StorageTierTransitionMatrix.Tier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageTierTransitionMatrixTest {
    @Test
    void testTierTransitions() {
        assertTrue(StorageTierTransitionMatrix.canTransition(Tier.HOT, Tier.WARM));
        assertTrue(StorageTierTransitionMatrix.canTransition(Tier.WARM, Tier.ARCHIVE));
        assertTrue(StorageTierTransitionMatrix.canTransition(Tier.HOT, Tier.HOT));
        assertFalse(StorageTierTransitionMatrix.canTransition(Tier.ARCHIVE, Tier.WARM));
    }
}
