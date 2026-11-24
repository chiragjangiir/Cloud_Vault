package com.cloudvault.unit;

import com.cloudvault.service.StorageTierRetrievalLatencyEstimator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageTierRetrievalLatencyEstimatorTest {
    @Test
    void testLatency() {
        assertEquals(0, StorageTierRetrievalLatencyEstimator.getMinRetrievalSeconds("STANDARD"));
        assertEquals(10800, StorageTierRetrievalLatencyEstimator.getMinRetrievalSeconds("GLACIER"));
        assertEquals(43200, StorageTierRetrievalLatencyEstimator.getMinRetrievalSeconds("DEEP_ARCHIVE"));
    }
}
