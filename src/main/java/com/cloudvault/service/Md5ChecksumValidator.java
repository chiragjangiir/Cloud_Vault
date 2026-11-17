package com.cloudvault.service;

public class Md5ChecksumValidator {
    public static boolean isValidEtag(String etag) {
        if (etag == null || etag.trim().isEmpty()) return false;
        String clean = etag.replace("\"", "").trim();
        return clean.matches("^[a-fA-F0-9]{32}(-[0-9]+)?$");
    }
}
