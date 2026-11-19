package com.cloudvault.service;

public enum AuditActionEnum {
    OBJECT_CREATE("WRITE"),
    OBJECT_READ("READ"),
    OBJECT_DELETE("DELETE"),
    BUCKET_ACL_UPDATE("SECURITY");

    private final String category;
    AuditActionEnum(String category) {
        this.category = category;
    }
    public String getCategory() {
        return category;
    }
}
