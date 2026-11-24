package com.cloudvault.unit;

import com.cloudvault.service.S3ContentEncodingValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3ContentEncodingValidatorTest {
    @Test
    void testEncoding() {
        assertTrue(S3ContentEncodingValidator.isSupportedEncoding("gzip"));
        assertTrue(S3ContentEncodingValidator.isSupportedEncoding("br"));
        assertFalse(S3ContentEncodingValidator.isSupportedEncoding("unknown_algo"));
    }
}
