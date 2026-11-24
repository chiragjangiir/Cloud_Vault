package com.cloudvault.service;

public final class BucketCannedAclParser {
    public enum CannedAcl {
        PRIVATE, PUBLIC_READ, PUBLIC_READ_WRITE, AUTHENTICATED_READ, UNKNOWN
    }

    private BucketCannedAclParser() {}

    public static CannedAcl parse(String raw) {
        if (raw == null) return CannedAcl.PRIVATE;
        String clean = raw.trim().toLowerCase().replace("-", "_");
        switch (clean) {
            case "private": return CannedAcl.PRIVATE;
            case "public_read": return CannedAcl.PUBLIC_READ;
            case "public_read_write": return CannedAcl.PUBLIC_READ_WRITE;
            case "authenticated_read": return CannedAcl.AUTHENTICATED_READ;
            default: return CannedAcl.UNKNOWN;
        }
    }
}
