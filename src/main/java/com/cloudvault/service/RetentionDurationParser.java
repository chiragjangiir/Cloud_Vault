package com.cloudvault.service;

import java.time.Period;

public final class RetentionDurationParser {
    private RetentionDurationParser() {}

    public static long parseDays(String isoPeriod) {
        if (isoPeriod == null || isoPeriod.isBlank()) return 0;
        try {
            Period p = Period.parse(isoPeriod);
            return p.getYears() * 365L + p.getMonths() * 30L + p.getDays();
        } catch (Exception e) {
            return 0;
        }
    }
}
