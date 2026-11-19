package com.cloudvault.unit;

import com.cloudvault.service.ContentEncodingHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentEncodingHelperTest {
    @Test
    void testContentEncoding() {
        assertTrue(ContentEncodingHelper.isSupported("gzip"));
        assertTrue(ContentEncodingHelper.isSupported("BR"));
        assertFalse(ContentEncodingHelper.isSupported("unknown"));
    }
}
