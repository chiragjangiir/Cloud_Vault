package com.cloudvault.unit;

import com.cloudvault.service.AuditActionEnum;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuditActionEnumTest {
    @Test
    void testAuditCategories() {
        assertEquals("WRITE", AuditActionEnum.OBJECT_CREATE.getCategory());
        assertEquals("SECURITY", AuditActionEnum.BUCKET_ACL_UPDATE.getCategory());
    }
}
