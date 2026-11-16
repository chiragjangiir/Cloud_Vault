package com.cloudvault.service;

import java.util.concurrent.ConcurrentLinkedQueue;

public class StreamBufferPool {
    private final int bufferSize;
    private final ConcurrentLinkedQueue<byte[]> pool = new ConcurrentLinkedQueue<>();

    public StreamBufferPool(int bufferSize) {
        this.bufferSize = Math.max(1024, bufferSize);
    }

    public byte[] acquire() {
        byte[] buf = pool.poll();
        if (buf == null) {
            return new byte[bufferSize];
        }
        return buf;
    }

    public void release(byte[] buf) {
        if (buf != null && buf.length == bufferSize) {
            pool.offer(buf);
        }
    }

    public int getPoolSize() {
        return pool.size();
    }
}
