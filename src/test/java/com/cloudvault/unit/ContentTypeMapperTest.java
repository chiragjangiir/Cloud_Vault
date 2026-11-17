package com.cloudvault.unit;

import com.cloudvault.service.ContentTypeMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ContentTypeMapperTest {
    @Test
    void testContentTypeLookup() {
        assertEquals("application/json", ContentTypeMapper.getContentType("json", "application/octet-stream"));
        assertEquals("image/svg+xml", ContentTypeMapper.getContentType("SVG", "application/octet-stream"));
        assertEquals("application/octet-stream", ContentTypeMapper.getContentType("unknown", "application/octet-stream"));
    }
}
