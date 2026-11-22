package com.cloudvault.unit;

import com.cloudvault.service.ContentDispositionFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentDispositionFormatterTest {
    @Test
    void testFormat() {
        assertEquals("attachment; filename=\"report.pdf\"", ContentDispositionFormatter.formatAttachment("report.pdf"));
        assertEquals("inline; filename=\"image.png\"", ContentDispositionFormatter.formatInline("image.png"));
        assertEquals("attachment", ContentDispositionFormatter.formatAttachment(""));
    }
}
