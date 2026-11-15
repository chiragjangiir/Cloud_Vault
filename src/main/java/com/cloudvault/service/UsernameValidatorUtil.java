package com.cloudvault.service;

import java.util.Set;
import java.util.regex.Pattern;

public final class UsernameValidatorUtil {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");
    private static final Set<String> RESERVED = Set.of("admin", "root", "system", "anonymous", "null");

    private UsernameValidatorUtil() {}

    public static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) return false;
        String clean = username.trim().toLowerCase();
        if (RESERVED.contains(clean)) return false;
        return USERNAME_PATTERN.matcher(clean).matches();
    }
}
