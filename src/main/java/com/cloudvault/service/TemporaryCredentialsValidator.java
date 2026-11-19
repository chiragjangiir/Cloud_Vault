package com.cloudvault.service;

import java.time.Instant;

public record TemporaryCredentialsValidator(String accessKeyId, String sessionToken, Instant expiration) {
    public boolean isValid(Instant now) {
        if (accessKeyId == null || sessionToken == null || expiration == null) return false;
        return accessKeyId.startsWith("ASIA") && expiration.isAfter(now);
    }
}
