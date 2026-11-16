package com.cloudvault.unit;

import com.cloudvault.service.ContentDispositionUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ContentDispositionUtilTest {
    @Test
    void testAttachmentHeader() {
        String header = ContentDispositionUtil.format(false, "report 2026.pdf");
        assertTrue(header.startsWith("attachment; filename=\"report_2026.pdf\""));
        assertTrue(header.contains("filename*=UTF-8''report%202026.pdf"));
    }

    @Test
    void testInlineHeaderNoFilename() {
        assertEquals("inline", ContentDispositionUtil.format(true, null));
    }
}
