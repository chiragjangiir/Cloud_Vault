package com.cloudvault.unit;

import com.cloudvault.service.ChecksumAlgorithmSelector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChecksumAlgorithmSelectorTest {
    @Test
    void testAlgorithmSelection() {
        assertEquals(ChecksumAlgorithmSelector.SupportedAlgorithm.CRC32, ChecksumAlgorithmSelector.select("CRC32"));
        assertEquals(ChecksumAlgorithmSelector.SupportedAlgorithm.SHA256, ChecksumAlgorithmSelector.select(null));
        assertEquals(ChecksumAlgorithmSelector.SupportedAlgorithm.SHA1, ChecksumAlgorithmSelector.select("sha1"));
    }
}
