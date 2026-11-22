package com.cloudvault.unit;

import com.cloudvault.service.StorageTierThawPriority;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageTierThawPriorityTest {
    @Test
    void testFromString() {
        assertEquals(StorageTierThawPriority.Priority.EXPEDITED, StorageTierThawPriority.fromString("Expedited"));
        assertEquals(StorageTierThawPriority.Priority.BULK, StorageTierThawPriority.fromString("BULK"));
        assertEquals(StorageTierThawPriority.Priority.STANDARD, StorageTierThawPriority.fromString("UNKNOWN"));
        assertEquals(60, StorageTierThawPriority.Priority.EXPEDITED.getEstimatedMinutes());
    }
}
