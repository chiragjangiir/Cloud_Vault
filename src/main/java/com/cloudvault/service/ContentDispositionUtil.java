package com.cloudvault.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ContentDispositionUtil {
    public static String format(boolean inline, String filename) {
        String type = inline ? "inline" : "attachment";
        if (filename == null || filename.trim().isEmpty()) {
            return type;
        }
        String cleanAscii = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return type + "; filename=\"" + cleanAscii + "\"; filename*=UTF-8''" + encoded;
    }
}
