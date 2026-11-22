package com.cloudvault.service;

public final class StorageTierThawPriority {
    public enum Priority {
        EXPEDITED(60),
        STANDARD(300),
        BULK(720);

        private final int estimatedMinutes;
        Priority(int estimatedMinutes) {
            this.estimatedMinutes = estimatedMinutes;
        }

        public int getEstimatedMinutes() {
            return estimatedMinutes;
        }
    }

    private StorageTierThawPriority() {}

    public static Priority fromString(String raw) {
        if (raw == null) return Priority.STANDARD;
        try {
            return Priority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.STANDARD;
        }
    }
}
