package com.cloudvault.service;

import java.util.Locale;
import java.util.Set;

public final class AccessControlListHelper {
    private static final Set<String> CANNED_ACLS = Set.of(
        "private",
        "public-read",
        "public-read-write",
        "authenticated-read",
        "bucket-owner-read",
        "bucket-owner-full-control"
    );

    private AccessControlListHelper() {}

    public static boolean isValidCannedAcl(String acl) {
        if (acl == null) return false;
        return CANNED_ACLS.contains(acl.trim().toLowerCase(Locale.ROOT));
    }

    public static String normalizeAcl(String acl) {
        if (!isValidCannedAcl(acl)) {
            return "private";
        }
        return acl.trim().toLowerCase(Locale.ROOT);
    }
}
