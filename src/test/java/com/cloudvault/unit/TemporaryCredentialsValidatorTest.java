package com.cloudvault.unit;

import com.cloudvault.service.TemporaryCredentialsValidator;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class TemporaryCredentialsValidatorTest {
    @Test
    void testTempCredentials() {
        Instant now = Instant.now();
        TemporaryCredentialsValidator creds = new TemporaryCredentialsValidator("ASIAEXAMPLE123", "tok456", now.plusSeconds(300));
        assertTrue(creds.isValid(now));
        assertFalse(creds.isValid(now.plusSeconds(400)));
    }
}
