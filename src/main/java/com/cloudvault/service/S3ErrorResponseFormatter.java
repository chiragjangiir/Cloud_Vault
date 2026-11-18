package com.cloudvault.service;

public final class S3ErrorResponseFormatter {
    private S3ErrorResponseFormatter() {}

    public static String formatErrorXml(String code, String message, String resource, String requestId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<Error>\n" +
               "    <Code>" + escapeXml(code) + "</Code>\n" +
               "    <Message>" + escapeXml(message) + "</Message>\n" +
               "    <Resource>" + escapeXml(resource) + "</Resource>\n" +
               "    <RequestId>" + escapeXml(requestId) + "</RequestId>\n" +
               "</Error>";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
