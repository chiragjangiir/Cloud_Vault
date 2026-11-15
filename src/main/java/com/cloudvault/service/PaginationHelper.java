package com.cloudvault.service;

/**
 * Clamps pagination query parameters to avoid denial-of-service via huge page sizes.
 */
public final class PaginationHelper {

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationHelper() {}

    public static int clampPage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    public static int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
