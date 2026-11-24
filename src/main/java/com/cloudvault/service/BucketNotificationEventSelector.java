package com.cloudvault.service;

public final class BucketNotificationEventSelector {
    private BucketNotificationEventSelector() {}

    public static boolean matchesEvent(String eventFilter, String actualEvent) {
        if (eventFilter == null || actualEvent == null) return false;
        String filter = eventFilter.trim();
        String actual = actualEvent.trim();
        if (filter.endsWith("*")) {
            String prefix = filter.substring(0, filter.length() - 1);
            return actual.startsWith(prefix);
        }
        return filter.equalsIgnoreCase(actual);
    }
}
