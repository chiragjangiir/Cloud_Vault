package com.cloudvault.service;

import java.text.Normalizer;
import java.util.Locale;

public final class UrlSafeSlugGenerator {
    private UrlSafeSlugGenerator() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-+|-+$", "");
    }
}
