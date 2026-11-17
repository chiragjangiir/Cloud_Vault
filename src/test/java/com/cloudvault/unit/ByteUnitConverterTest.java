package com.cloudvault.unit;

import com.cloudvault.service.ByteUnitConverter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ByteUnitConverterTest {
    @Test
    void testConversions() {
        assertEquals(1.0, ByteUnitConverter.toMegabytes(1024 * 1024));
        assertEquals(1.0, ByteUnitConverter.toGigabytes(1024L * 1024 * 1024));
        assertEquals(1073741824L, ByteUnitConverter.gigabytesToBytes(1.0));
    }
}
