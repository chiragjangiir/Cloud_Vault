package com.cloudvault.service;

public final class StorageTierRetrievalLatencyEstimator {
    private StorageTierRetrievalLatencyEstimator() {}

    public static int getMinRetrievalSeconds(String storageClass) {
        if (storageClass == null) return 0;
        String sc = storageClass.trim().toUpperCase();
        if ("GLACIER".equals(sc)) return 10800; // 3 hours
        if ("DEEP_ARCHIVE".equals(sc)) return 43200; // 12 hours
        return 0; // immediate
    }
}
