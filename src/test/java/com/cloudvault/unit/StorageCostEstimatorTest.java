package com.cloudvault.unit;

import com.cloudvault.service.StorageCostEstimator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StorageCostEstimatorTest {
    @Test
    void testCostEstimation() {
        double cost = StorageCostEstimator.estimateMonthlyCost(1000.0, false);
        assertEquals(23.0, cost, 0.001);
    }
}
