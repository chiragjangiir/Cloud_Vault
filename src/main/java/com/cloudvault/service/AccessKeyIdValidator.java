package com.cloudvault.service;

import java.util.regex.Pattern;

public final class AccessKeyIdValidator {
    private static final Pattern PATTERN = Pattern.compile("^(AKIA|ASIA)[A-Z0-9]{16}$");

    private AccessKeyIdValidator() {}

    public static boolean isValidKey(String key) {
        if (key == null) return false;
        return PATTERN.matcher(key).matches();
    }
}
