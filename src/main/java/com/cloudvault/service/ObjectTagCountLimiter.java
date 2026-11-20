package com.cloudvault.service;

import java.util.Map;

public final class ObjectTagCountLimiter {
    public static final int MAX_TAG_SET_SIZE = 10;

    private ObjectTagCountLimiter() {}

    public static boolean isWithinLimits(Map<String, String> tagSet) {
        if (tagSet == null) return true;
        return tagSet.size() <= MAX_TAG_SET_SIZE;
    }
}
