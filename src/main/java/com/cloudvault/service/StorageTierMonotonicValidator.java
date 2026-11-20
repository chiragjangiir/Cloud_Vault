package com.cloudvault.service;

import java.util.Map;

public final class StorageTierMonotonicValidator {
    private static final Map<String, Integer> RANKS = Map.of(
        "HOT", 1,
        "WARM", 2,
        "COLD", 3,
        "ARCHIVE", 4
    );

    private StorageTierMonotonicValidator() {}

    public static boolean isTransitionPermitted(String currentTier, String nextTier) {
        if (currentTier == null || nextTier == null) return false;
        int r1 = RANKS.getOrDefault(currentTier.toUpperCase(), 0);
        int r2 = RANKS.getOrDefault(nextTier.toUpperCase(), 0);
        return r1 > 0 && r2 >= r1;
    }
}
