package com.cloudvault.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class S3TaggingXmlParser {
    private static final Pattern TAG_PATTERN = Pattern.compile("<Tag>\\s*<Key>(.*?)</Key>\\s*<Value>(.*?)</Value>\\s*</Tag>");

    private S3TaggingXmlParser() {}

    public static Map<String, String> parseTags(String xml) {
        Map<String, String> tags = new HashMap<>();
        if (xml == null) return tags;
        Matcher m = TAG_PATTERN.matcher(xml);
        while (m.find()) {
            tags.put(m.group(1).trim(), m.group(2).trim());
        }
        return tags;
    }
}
