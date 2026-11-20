package com.cloudvault.unit;

import com.cloudvault.service.ContentMd5HeaderValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentMd5HeaderValidatorTest {
    @Test
    void testContentMd5Matching() {
        // MD5 of "hello world" in hex is 5eb63bbbe01eeed093cb22bb8f5acdc3
        // In base64 is XrY7u+Ae7tCTyyK7j1rNww==
        assertTrue(ContentMd5HeaderValidator.matches("XrY7u+Ae7tCTyyK7j1rNww==", "5eb63bbbe01eeed093cb22bb8f5acdc3"));
        assertFalse(ContentMd5HeaderValidator.matches("invalid", "5eb63bbbe01eeed093cb22bb8f5acdc3"));
    }
}
