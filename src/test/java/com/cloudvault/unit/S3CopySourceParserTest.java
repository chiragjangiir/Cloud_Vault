package com.cloudvault.unit;

import com.cloudvault.service.S3CopySourceParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class S3CopySourceParserTest {
    @Test
    void testParse() {
        S3CopySourceParser parsed = S3CopySourceParser.parse("/my-bucket/path/to/object.txt?versionId=v123");
        assertNotNull(parsed);
        assertEquals("my-bucket", parsed.getSourceBucket());
        assertEquals("path/to/object.txt", parsed.getSourceKey());
        assertEquals("v123", parsed.getVersionId());
    }
}
