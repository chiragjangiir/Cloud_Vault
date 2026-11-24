package com.cloudvault.unit;

import com.cloudvault.service.S3TaggingXmlParser;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class S3TaggingXmlParserTest {
    @Test
    void testParseTags() {
        String xml = "<Tagging><TagSet><Tag><Key>Environment</Key><Value>Production</Value></Tag></TagSet></Tagging>";
        Map<String, String> tags = S3TaggingXmlParser.parseTags(xml);
        assertEquals("Production", tags.get("Environment"));
    }
}
