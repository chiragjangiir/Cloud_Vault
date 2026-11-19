package com.cloudvault.unit;

import com.cloudvault.service.StorageMetricsSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageMetricsSnapshotTest {
    @Test
    void testMetrics() {
        StorageMetricsSnapshot snapshot = new StorageMetricsSnapshot("vault-1", 10, 1000);
        assertEquals(100.0, snapshot.getAverageObjectSize(), 0.01);
    }
}
