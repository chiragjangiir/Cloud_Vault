package com.cloudvault.unit;

import com.cloudvault.service.Format;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Formatting helpers — every dashboard number is derived from real values. */
class FormatTest {

    @Test
    void formatsByteSizes() {
        assertEquals("0 B", Format.bytes(0));
        assertEquals("512 B", Format.bytes(512));
        assertEquals("1023 B", Format.bytes(1023));
        assertEquals("1.00 KB", Format.bytes(1024));
        assertEquals("1.50 KB", Format.bytes(1536));
        assertEquals("10.0 KB", Format.bytes(10 * 1024));
        assertEquals("100 KB", Format.bytes(100 * 1024));
        assertEquals("1.00 MB", Format.bytes(1024L * 1024L));
        assertEquals("1.00 GB", Format.bytes(1024L * 1024L * 1024L));
        assertEquals("0 B", Format.bytes(-5));
    }

    @Test
    void formatsPercentagesFromRealNumbers() {
        assertEquals("0%", Format.percent(0, 0));
        assertEquals("50.0%", Format.percent(50, 100));
        assertEquals("100.0%", Format.percent(200, 100)); // clamped, never > 100%
        assertEquals(0.0, Format.percentNumber(5, 0));
        assertEquals(50.0, Format.percentNumber(1, 2));
        assertEquals(100.0, Format.percentNumber(5, 2));   // clamped
    }

    @Test
    void formatsTimestampsDeterministicallyInUtc() {
        assertEquals("—", Format.dateTime(null));
        assertEquals("—", Format.date(null));
        // Fixed instant → stable string (2020-01-01T00:00:00Z)
        assertEquals("2020-01-01 00:00", Format.dateTime(Instant.parse("2020-01-01T00:00:00Z")));
        assertEquals("2020-01-01", Format.date(Instant.parse("2020-01-01T12:34:56Z")));
        assertTrue(Format.dateTime(Instant.now()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
    }
}
