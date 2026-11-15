package com.cloudvault.unit;

import com.cloudvault.service.PaginationHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationHelperTest {

    @Test
    void testClampPage() {
        assertEquals(0, PaginationHelper.clampPage(null));
        assertEquals(0, PaginationHelper.clampPage(-5));
        assertEquals(3, PaginationHelper.clampPage(3));
    }

    @Test
    void testClampSize() {
        assertEquals(PaginationHelper.DEFAULT_PAGE_SIZE, PaginationHelper.clampSize(null));
        assertEquals(PaginationHelper.DEFAULT_PAGE_SIZE, PaginationHelper.clampSize(0));
        assertEquals(PaginationHelper.DEFAULT_PAGE_SIZE, PaginationHelper.clampSize(-10));
        assertEquals(50, PaginationHelper.clampSize(50));
        assertEquals(PaginationHelper.MAX_PAGE_SIZE, PaginationHelper.clampSize(5000));
    }
}
