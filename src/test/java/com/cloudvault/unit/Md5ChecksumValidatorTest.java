package com.cloudvault.unit;

import com.cloudvault.service.Md5ChecksumValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Md5ChecksumValidatorTest {
    @Test
    void testValidEtags() {
        assertTrue(Md5ChecksumValidator.isValidEtag("d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(Md5ChecksumValidator.isValidEtag("\"d41d8cd98f00b204e9800998ecf8427e-4\""));
    }

    @Test
    void testInvalidEtags() {
        assertFalse(Md5ChecksumValidator.isValidEtag("not-an-etag"));
        assertFalse(Md5ChecksumValidator.isValidEtag(null));
    }
}
