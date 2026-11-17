package com.cloudvault.service;

import java.util.concurrent.Semaphore;

public class ConcurrentTransferLimiter {
    private final Semaphore semaphore;

    public ConcurrentTransferLimiter(int maxConcurrent) {
        this.semaphore = new Semaphore(Math.max(1, maxConcurrent));
    }

    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public void release() {
        semaphore.release();
    }

    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }
}
