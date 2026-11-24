package com.cloudvault.unit;

import com.cloudvault.service.S3MetricsFilterIdValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3MetricsFilterIdValidatorTest {
    @Test
    void testFilterId() {
        assertTrue(S3MetricsFilterIdValidator.isValidFilterId("documents_2026"));
        assertTrue(S3MetricsFilterIdValidator.isValidFilterId("filter-1"));
        assertFalse(S3MetricsFilterIdValidator.isValidFilterId("bad filter"));
    }
}
