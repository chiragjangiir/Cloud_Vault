package com.cloudvault.unit;

import com.cloudvault.service.S3XmlListBucketFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3XmlListBucketFormatterTest {
    @Test
    void testXmlFormatting() {
        String xml = S3XmlListBucketFormatter.formatHeader("my-bucket", 1000);
        assertTrue(xml.contains("<Name>my-bucket</Name>"));
        assertTrue(xml.contains("<MaxKeys>1000</MaxKeys>"));
    }
}
