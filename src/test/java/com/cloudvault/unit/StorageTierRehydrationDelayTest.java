package com.cloudvault.unit;

import com.cloudvault.service.StorageTierRehydrationDelay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageTierRehydrationDelayTest {
    @Test
    void testRehydrationDelays() {
        assertEquals(1, StorageTierRehydrationDelay.getEstimatedHours("COLD", StorageTierRehydrationDelay.Priority.EXPEDITED));
        assertEquals(12, StorageTierRehydrationDelay.getEstimatedHours("ARCHIVE", StorageTierRehydrationDelay.Priority.STANDARD));
        assertEquals(0, StorageTierRehydrationDelay.getEstimatedHours("HOT", StorageTierRehydrationDelay.Priority.STANDARD));
    }
}
