package com.cloudvault.service;

import java.util.regex.Pattern;

public final class BucketNameValidator {
    private static final Pattern BUCKET_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");
    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

    private BucketNameValidator() {}

    public static boolean isValid(String name) {
        if (name == null || name.length() < 3 || name.length() > 63) {
            return false;
        }
        if (!BUCKET_PATTERN.matcher(name).matches()) {
            return false;
        }
        if (name.contains("..") || name.contains(".-") || name.contains("-.")) {
            return false;
        }
        if (IP_PATTERN.matcher(name).matches()) {
            return false;
        }
        return true;
    }
}
