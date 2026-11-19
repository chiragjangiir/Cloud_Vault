package com.cloudvault.unit;

import com.cloudvault.service.EtagGenerationUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EtagGenerationUtilTest {
    @Test
    void testEtagFormatting() {
        assertEquals("\"c0ffee\"", EtagGenerationUtil.formatSinglePartEtag("c0ffee"));
        assertEquals("\"c0ffee-4\"", EtagGenerationUtil.formatMultipartEtag("c0ffee", 4));
    }
}
