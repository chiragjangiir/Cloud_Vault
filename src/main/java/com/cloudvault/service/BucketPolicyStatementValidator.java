package com.cloudvault.service;

import java.util.List;

public record BucketPolicyStatementValidator(String effect, List<String> actions, String resource) {
    public boolean isValid() {
        if (effect == null || (!"Allow".equals(effect) && !"Deny".equals(effect))) return false;
        if (actions == null || actions.isEmpty()) return false;
        return resource != null && resource.startsWith("arn:aws:s3:::");
    }
}
