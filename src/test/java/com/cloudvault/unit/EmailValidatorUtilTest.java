package com.cloudvault.unit;

import com.cloudvault.service.EmailValidatorUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorUtilTest {
    @Test
    void testEmailValidation() {
        assertTrue(EmailValidatorUtil.isValidEmail("user@example.com"));
        assertTrue(EmailValidatorUtil.isValidEmail("alice.smith+tag@domain.co.uk"));
        assertFalse(EmailValidatorUtil.isValidEmail("plainaddress"));
        assertFalse(EmailValidatorUtil.isValidEmail("@missingusername.com"));
        assertFalse(EmailValidatorUtil.isValidEmail(""));
    }
}
