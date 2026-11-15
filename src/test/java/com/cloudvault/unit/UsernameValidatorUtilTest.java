package com.cloudvault.unit;

import com.cloudvault.service.UsernameValidatorUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsernameValidatorUtilTest {
    @Test
    void testUsernameValidation() {
        assertTrue(UsernameValidatorUtil.isValidUsername("chirag_dev"));
        assertFalse(UsernameValidatorUtil.isValidUsername("admin"));
        assertFalse(UsernameValidatorUtil.isValidUsername("ab")); // too short
        assertFalse(UsernameValidatorUtil.isValidUsername("invalid user")); // spaces
    }
}
