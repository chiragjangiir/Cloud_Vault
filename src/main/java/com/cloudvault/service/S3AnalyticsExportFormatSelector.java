package com.cloudvault.service;

public final class S3AnalyticsExportFormatSelector {
    public enum Format {
        CSV, PARQUET, ORC
    }

    private S3AnalyticsExportFormatSelector() {}

    public static Format select(String raw) {
        if (raw == null) return Format.CSV;
        String clean = raw.trim().toUpperCase();
        for (Format f : Format.values()) {
            if (f.name().equals(clean)) return f;
        }
        return Format.CSV;
    }
}
