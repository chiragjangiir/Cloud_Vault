package com.cloudvault.unit;

import com.cloudvault.service.StorageCostForecastUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageCostForecastUtilTest {
    @Test
    void testCostForecast() {
        assertEquals(23.0, StorageCostForecastUtil.forecastMonthlyCost(1000, 0.023), 0.001);
        assertEquals(0.0, StorageCostForecastUtil.forecastMonthlyCost(0, 0.023));
    }
}
