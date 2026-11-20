package com.cloudvault.unit;

import com.cloudvault.service.S3ErrorXmlResponseBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3ErrorXmlResponseBuilderTest {
    @Test
    void testBuildError() {
        String xml = S3ErrorXmlResponseBuilder.build("AccessDenied", "Denied", "host-1");
        assertTrue(xml.contains("<Code>AccessDenied</Code>"));
    }
}
