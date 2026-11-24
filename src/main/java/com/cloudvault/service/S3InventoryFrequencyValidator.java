package com.cloudvault.service;

public final class S3InventoryFrequencyValidator {
    public enum Frequency {
        DAILY, WEEKLY
    }

    private S3InventoryFrequencyValidator() {}

    public static Frequency parse(String raw) {
        if (raw == null) return null;
        String clean = raw.trim().toUpperCase();
        if ("DAILY".equals(clean)) return Frequency.DAILY;
        if ("WEEKLY".equals(clean)) return Frequency.WEEKLY;
        return null;
    }
}
