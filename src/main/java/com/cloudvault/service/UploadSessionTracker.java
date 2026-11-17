package com.cloudvault.service;

import java.util.concurrent.ConcurrentHashMap;

public class UploadSessionTracker {
    private final ConcurrentHashMap<String, Long> sessions = new ConcurrentHashMap<>();

    public void startSession(String uploadId) {
        sessions.put(uploadId, System.currentTimeMillis());
    }

    public boolean isActive(String uploadId) {
        return sessions.containsKey(uploadId);
    }

    public void endSession(String uploadId) {
        sessions.remove(uploadId);
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
