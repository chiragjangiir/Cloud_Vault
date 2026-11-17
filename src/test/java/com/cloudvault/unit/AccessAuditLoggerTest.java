package com.cloudvault.unit;

import com.cloudvault.service.AccessAuditLogger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AccessAuditLoggerTest {
    @Test
    void testFormatAuditEvent() {
        String log = AccessAuditLogger.formatAuditEvent("t1", "DOWNLOAD", "files/a.pdf", "192.168.1.1");
        assertEquals("ACCESS_AUDIT tenant=t1 action=DOWNLOAD key=files/a.pdf ip=192.168.1.1", log);
    }
}
