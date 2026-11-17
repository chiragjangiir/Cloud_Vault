package com.cloudvault.service;

public class FileIntegrityManifest {
    private final String sha256;
    private final String md5;
    private final long sizeBytes;

    public FileIntegrityManifest(String sha256, String md5, long sizeBytes) {
        this.sha256 = sha256;
        this.md5 = md5;
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() { return sha256; }
    public String getMd5() { return md5; }
    public long getSizeBytes() { return sizeBytes; }
}
