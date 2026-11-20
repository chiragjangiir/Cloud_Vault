package com.cloudvault.service;

import java.util.Base64;

public final class ContentMd5HeaderValidator {
    private ContentMd5HeaderValidator() {}

    public static boolean matches(String base64Md5, String hexMd5) {
        if (base64Md5 == null || hexMd5 == null) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Md5.trim());
            StringBuilder sb = new StringBuilder();
            for (byte b : decoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(hexMd5.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
