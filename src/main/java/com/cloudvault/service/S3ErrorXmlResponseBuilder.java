package com.cloudvault.service;

public final class S3ErrorXmlResponseBuilder {
    private S3ErrorXmlResponseBuilder() {}

    public static String build(String code, String message, String hostId) {
        return "<Error>" +
               "<Code>" + code + "</Code>" +
               "<Message>" + message + "</Message>" +
               "<HostId>" + hostId + "</HostId>" +
               "</Error>";
    }
}
