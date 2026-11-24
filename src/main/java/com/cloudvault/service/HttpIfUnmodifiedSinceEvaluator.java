package com.cloudvault.service;

import java.time.Instant;

public final class HttpIfUnmodifiedSinceEvaluator {
    private HttpIfUnmodifiedSinceEvaluator() {}

    public static boolean isUnmodified(Instant lastModified, Instant ifUnmodifiedSince) {
        if (lastModified == null || ifUnmodifiedSince == null) return true;
        return !lastModified.isAfter(ifUnmodifiedSince);
    }
}
