package com.cloudvault.unit;

import com.cloudvault.service.UrlSafeSlugGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrlSafeSlugGeneratorTest {
    @Test
    void testSlugGeneration() {
        assertEquals("financial-report-2026", UrlSafeSlugGenerator.toSlug("Financial Report 2026!"));
        assertEquals("cafe-menu", UrlSafeSlugGenerator.toSlug("Café Menu"));
        assertEquals("", UrlSafeSlugGenerator.toSlug("   "));
    }
}
