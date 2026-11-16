package com.cloudvault.unit;

import com.cloudvault.service.HexEncodeUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HexEncodeUtilTest {
    @Test
    void testRoundTrip() {
        byte[] raw = new byte[]{0x00, 0x0F, (byte) 0xFF, 0x42};
        String hex = HexEncodeUtil.toHexString(raw);
        assertEquals("000fff42", hex);
        byte[] decoded = HexEncodeUtil.fromHexString(hex);
        assertArrayEquals(raw, decoded);
    }

    @Test
    void testOddLengthThrows() {
        assertThrows(IllegalArgumentException.class, () -> HexEncodeUtil.fromHexString("abc"));
    }
}
