package com.cloudvault.service;

import java.util.Base64;

public class Base64SafeUrlEncoder {
    public static String encode(byte[] data) {
        if (data == null) return "";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static byte[] decode(String s) {
        if (s == null || s.isEmpty()) return new byte[0];
        return Base64.getUrlDecoder().decode(s);
    }
}
