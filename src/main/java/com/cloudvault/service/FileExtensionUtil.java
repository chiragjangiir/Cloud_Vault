package com.cloudvault.service;

public class FileExtensionUtil {
    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String nameOnly = (lastSlash >= 0) ? filename.substring(lastSlash + 1) : filename;
        int dotIndex = nameOnly.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == nameOnly.length() - 1) {
            return "";
        }
        return nameOnly.substring(dotIndex + 1).toLowerCase();
    }

    public static boolean isArchive(String filename) {
        String ext = getExtension(filename);
        return ext.equals("zip") || ext.equals("tar") || ext.equals("gz") || ext.equals("7z") || ext.equals("rar");
    }

    public static boolean isImage(String filename) {
        String ext = getExtension(filename);
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("webp");
    }
}
