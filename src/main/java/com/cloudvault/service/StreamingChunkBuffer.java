package com.cloudvault.service;

import java.io.ByteArrayOutputStream;

public class StreamingChunkBuffer {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final int targetChunkSize;

    public StreamingChunkBuffer(int targetChunkSize) {
        this.targetChunkSize = targetChunkSize;
    }

    public synchronized void write(byte[] data, int offset, int length) {
        buffer.write(data, offset, length);
    }

    public synchronized boolean isReady() {
        return buffer.size() >= targetChunkSize;
    }

    public synchronized byte[] flush() {
        byte[] bytes = buffer.toByteArray();
        buffer.reset();
        return bytes;
    }

    public synchronized int size() {
        return buffer.size();
    }
}
