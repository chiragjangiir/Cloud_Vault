package com.cloudvault.service;

import java.util.HashMap;
import java.util.Map;

public final class SignedQueryParamExtractor {
    private SignedQueryParamExtractor() {}

    public static Map<String, String> extractAmzParams(Map<String, String> allParams) {
        Map<String, String> filtered = new HashMap<>();
        if (allParams == null) return filtered;
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith("x-amz-")) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }
}
