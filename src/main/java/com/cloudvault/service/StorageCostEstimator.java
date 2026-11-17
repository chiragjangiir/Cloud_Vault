package com.cloudvault.service;

public class StorageCostEstimator {
    public static final double STANDARD_RATE_PER_GB_MONTH = 0.023;
    public static final double ARCHIVE_RATE_PER_GB_MONTH = 0.004;

    public static double estimateMonthlyCost(double totalGigabytes, boolean isArchive) {
        double rate = isArchive ? ARCHIVE_RATE_PER_GB_MONTH : STANDARD_RATE_PER_GB_MONTH;
        return totalGigabytes * rate;
    }
}
