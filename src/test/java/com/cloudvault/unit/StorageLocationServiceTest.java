package com.cloudvault.unit;

import com.cloudvault.service.StorageLocationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageLocationServiceTest {

    @Test
    void testCalculateUtilizationPercentage() {
        assertEquals(0.0, StorageLocationService.calculateUtilizationPercentage(0, 1000));
        assertEquals(0.0, StorageLocationService.calculateUtilizationPercentage(-10, 1000));
        assertEquals(0.0, StorageLocationService.calculateUtilizationPercentage(100, 0));
        assertEquals(50.0, StorageLocationService.calculateUtilizationPercentage(500, 1000));
        assertEquals(75.5, StorageLocationService.calculateUtilizationPercentage(755, 1000));
        assertEquals(100.0, StorageLocationService.calculateUtilizationPercentage(1200, 1000));
    }
}
