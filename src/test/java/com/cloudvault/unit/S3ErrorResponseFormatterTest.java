package com.cloudvault.unit;

import com.cloudvault.service.S3ErrorResponseFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3ErrorResponseFormatterTest {
    @Test
    void testXmlFormatting() {
        String xml = S3ErrorResponseFormatter.formatErrorXml("NoSuchKey", "The key does not exist", "/bucket/file.txt", "tx123");
        assertTrue(xml.contains("<Code>NoSuchKey</Code>"));
        assertTrue(xml.contains("<Message>The key does not exist</Message>"));
        assertTrue(xml.contains("<Resource>/bucket/file.txt</Resource>"));
    }
}
