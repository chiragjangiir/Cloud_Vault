package com.cloudvault.unit;

import com.cloudvault.service.StorageClassTransitionCostComparator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageClassTransitionCostComparatorTest {
    @Test
    void testTransition() {
        assertTrue(StorageClassTransitionCostComparator.isValidDownwardTransition(
            StorageClassTransitionCostComparator.TierRank.STANDARD,
            StorageClassTransitionCostComparator.TierRank.GLACIER
        ));
        assertFalse(StorageClassTransitionCostComparator.isValidDownwardTransition(
            StorageClassTransitionCostComparator.TierRank.DEEP_ARCHIVE,
            StorageClassTransitionCostComparator.TierRank.STANDARD
        ));
    }
}
