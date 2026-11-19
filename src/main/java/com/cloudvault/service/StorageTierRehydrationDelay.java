package com.cloudvault.service;

public final class StorageTierRehydrationDelay {
    public enum Priority {
        EXPEDITED,
        STANDARD,
        BULK
    }

    private StorageTierRehydrationDelay() {}

    public static int getEstimatedHours(String tier, Priority priority) {
        if (tier == null) return 0;
        String t = tier.toUpperCase();
        if ("COLD".equals(t)) {
            return priority == Priority.EXPEDITED ? 1 : 5;
        } else if ("ARCHIVE".equals(t) || "DEEP_ARCHIVE".equals(t)) {
            return priority == Priority.EXPEDITED ? 2 : (priority == Priority.STANDARD ? 12 : 48);
        }
        return 0; // HOT or WARM instant
    }
}
