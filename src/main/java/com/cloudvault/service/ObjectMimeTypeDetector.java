package com.cloudvault.service;

public final class ObjectMimeTypeDetector {
    private ObjectMimeTypeDetector() {}

    public static String detectFromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream";
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "json": return "application/json";
            case "xml": return "application/xml";
            case "pdf": return "application/pdf";
            case "txt": return "text/plain";
            case "html": return "text/html";
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            default: return "application/octet-stream";
        }
    }
}
