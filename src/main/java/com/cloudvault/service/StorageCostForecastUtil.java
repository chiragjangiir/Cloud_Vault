package com.cloudvault.service;

public final class StorageCostForecastUtil {
    private StorageCostForecastUtil() {}

    public static double forecastMonthlyCost(long gigabytes, double ratePerGbMonth) {
        if (gigabytes <= 0 || ratePerGbMonth <= 0) return 0.0;
        return gigabytes * ratePerGbMonth;
    }
}
