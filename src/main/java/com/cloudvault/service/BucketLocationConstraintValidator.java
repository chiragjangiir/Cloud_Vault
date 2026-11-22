package com.cloudvault.service;

import java.util.regex.Pattern;

public final class BucketLocationConstraintValidator {
    private static final Pattern REGION_PATTERN = Pattern.compile("^[a-z]{2}-[a-z]+-\\d+$");

    private BucketLocationConstraintValidator() {}

    public static boolean isValidRegion(String region) {
        if (region == null) return false;
        String clean = region.trim();
        if ("us-east-1".equals(clean)) return true;
        return REGION_PATTERN.matcher(clean).matches();
    }
}
