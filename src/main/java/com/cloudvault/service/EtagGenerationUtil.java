package com.cloudvault.service;

public final class EtagGenerationUtil {
    private EtagGenerationUtil() {}

    public static String formatSinglePartEtag(String md5Hex) {
        return "\"" + md5Hex + "\"";
    }

    public static String formatMultipartEtag(String compositeMd5Hex, int partCount) {
        return "\"" + compositeMd5Hex + "-" + partCount + "\"";
    }
}
