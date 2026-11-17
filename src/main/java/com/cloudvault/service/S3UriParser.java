package com.cloudvault.service;

public class S3UriParser {
    public static class S3Location {
        public final String bucket;
        public final String key;

        public S3Location(String bucket, String key) {
            this.bucket = bucket;
            this.key = key;
        }
    }

    public static S3Location parse(String uri) {
        if (uri == null || !uri.startsWith("s3://")) {
            throw new IllegalArgumentException("Invalid S3 URI: " + uri);
        }
        String stripped = uri.substring(5);
        int slash = stripped.indexOf('/');
        if (slash <= 0) {
            throw new IllegalArgumentException("S3 URI missing key: " + uri);
        }
        String bucket = stripped.substring(0, slash);
        String key = stripped.substring(slash + 1);
        return new S3Location(bucket, key);
    }
}
