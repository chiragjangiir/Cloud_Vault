package com.cloudvault.service;

public final class BucketLifecycleDaysValidator {
    private BucketLifecycleDaysValidator() {}

    public static boolean isValidLifecycleTransition(int transitionDays, int expirationDays) {
        if (transitionDays <= 0 || expirationDays <= 0) return false;
        return expirationDays > transitionDays;
    }
}
