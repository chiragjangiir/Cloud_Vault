package com.cloudvault.unit;

import com.cloudvault.service.BucketCannedAclParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BucketCannedAclParserTest {
    @Test
    void testAclParser() {
        assertEquals(BucketCannedAclParser.CannedAcl.PRIVATE, BucketCannedAclParser.parse("private"));
        assertEquals(BucketCannedAclParser.CannedAcl.PUBLIC_READ, BucketCannedAclParser.parse("public-read"));
        assertEquals(BucketCannedAclParser.CannedAcl.UNKNOWN, BucketCannedAclParser.parse("invalid-acl"));
    }
}
