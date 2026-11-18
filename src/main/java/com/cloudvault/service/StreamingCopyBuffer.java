package com.cloudvault.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamingCopyBuffer {
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private StreamingCopyBuffer() {}

    public static long copy(InputStream source, OutputStream target, long maxAllowedBytes) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long totalRead = 0;
        int read;
        while ((read = source.read(buffer)) != -1) {
            totalRead += read;
            if (maxAllowedBytes > 0 && totalRead > maxAllowedBytes) {
                throw new IOException("Stream exceeded max allowed bytes: " + maxAllowedBytes);
            }
            target.write(buffer, 0, read);
        }
        return totalRead;
    }
}
