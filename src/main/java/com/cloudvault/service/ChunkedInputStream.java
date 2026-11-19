package com.cloudvault.service;

import java.io.IOException;
import java.io.InputStream;

public class ChunkedInputStream extends InputStream {
    private final InputStream delegate;
    private final long maxBytes;
    private long bytesRead = 0;

    public ChunkedInputStream(InputStream delegate, long maxBytes) {
        this.delegate = delegate;
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        if (bytesRead >= maxBytes) return -1;
        int b = delegate.read();
        if (b != -1) bytesRead++;
        return b;
    }
}
