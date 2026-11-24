package com.cloudvault.unit;

import com.cloudvault.service.S3AnalyticsExportFormatSelector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3AnalyticsExportFormatSelectorTest {
    @Test
    void testAnalyticsFormat() {
        assertEquals(S3AnalyticsExportFormatSelector.Format.PARQUET, S3AnalyticsExportFormatSelector.select("PARQUET"));
        assertEquals(S3AnalyticsExportFormatSelector.Format.CSV, S3AnalyticsExportFormatSelector.select("unknown"));
    }
}
