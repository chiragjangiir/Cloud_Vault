package com.cloudvault.service;

import java.util.regex.Pattern;

public final class S3MetricsFilterIdValidator {
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private S3MetricsFilterIdValidator() {}

    public static boolean isValidFilterId(String filterId) {
        if (filterId == null) return false;
        return ID_PATTERN.matcher(filterId.trim()).matches();
    }
}
