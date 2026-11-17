package com.cloudvault.unit;

import com.cloudvault.service.S3UriParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class S3UriParserTest {
    @Test
    void testParseS3Uri() {
        S3UriParser.S3Location loc = S3UriParser.parse("s3://my-bucket/path/to/object.pdf");
        assertEquals("my-bucket", loc.bucket);
        assertEquals("path/to/object.pdf", loc.key);
    }

    @Test
    void testInvalidS3UriThrows() {
        assertThrows(IllegalArgumentException.class, () -> S3UriParser.parse("https://example.com/file"));
    }
}
