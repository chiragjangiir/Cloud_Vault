package com.cloudvault.service;

public final class S3CopySourceParser {
    private final String sourceBucket;
    private final String sourceKey;
    private final String versionId;

    public S3CopySourceParser(String sourceBucket, String sourceKey, String versionId) {
        this.sourceBucket = sourceBucket;
        this.sourceKey = sourceKey;
        this.versionId = versionId;
    }

    public static S3CopySourceParser parse(String headerValue) {
        if (headerValue == null) return null;
        String clean = headerValue.trim();
        if (clean.startsWith("/")) clean = clean.substring(1);
        String versionId = null;
        int queryIdx = clean.indexOf('?');
        if (queryIdx >= 0) {
            String query = clean.substring(queryIdx + 1);
            clean = clean.substring(0, queryIdx);
            if (query.startsWith("versionId=")) {
                versionId = query.substring(10);
            }
        }
        int slashIdx = clean.indexOf('/');
        if (slashIdx <= 0) return null;
        String bucket = clean.substring(0, slashIdx);
        String key = clean.substring(slashIdx + 1);
        return new S3CopySourceParser(bucket, key, versionId);
    }

    public String getSourceBucket() { return sourceBucket; }
    public String getSourceKey() { return sourceKey; }
    public String getVersionId() { return versionId; }
}
