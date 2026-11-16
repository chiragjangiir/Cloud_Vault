package com.cloudvault.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class StorageKeyGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public static String generateKey(String tenantId, String originalFilename) {
        String tenant = (tenantId == null || tenantId.isEmpty()) ? "default" : tenantId.replaceAll("[^a-zA-Z0-9_-]", "");
        String datePrefix = LocalDate.now().format(DATE_FORMAT);
        String uuid = UUID.randomUUID().toString();
        String ext = FileExtensionUtil.getExtension(originalFilename);
        String extensionSuffix = ext.isEmpty() ? "" : "." + ext;
        return tenant + "/" + datePrefix + "/" + uuid + extensionSuffix;
    }
}
