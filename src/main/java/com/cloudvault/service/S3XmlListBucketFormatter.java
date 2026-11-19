package com.cloudvault.service;

public final class S3XmlListBucketFormatter {
    private S3XmlListBucketFormatter() {}

    public static String formatHeader(String bucketName, int maxKeys) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ListBucketResult>\n" +
               "    <Name>" + bucketName + "</Name>\n" +
               "    <MaxKeys>" + maxKeys + "</MaxKeys>\n" +
               "</ListBucketResult>";
    }
}
