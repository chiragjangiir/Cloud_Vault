package com.cloudvault.unit;

import com.cloudvault.service.DurationFormatUtil;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationFormatUtilTest {
    @Test
    void testDurationFormatting() {
        assertEquals("0s", DurationFormatUtil.formatDuration(Duration.ZERO));
        assertEquals("45s", DurationFormatUtil.formatDuration(Duration.ofSeconds(45)));
        assertEquals("2m", DurationFormatUtil.formatDuration(Duration.ofSeconds(120)));
        assertEquals("2m 15s", DurationFormatUtil.formatDuration(Duration.ofSeconds(135)));
        assertEquals("1h 30m", DurationFormatUtil.formatDuration(Duration.ofMinutes(90)));
    }
}
