package com.cloudvault.service;

public final class S3SelectExpressionValidator {
    private S3SelectExpressionValidator() {}

    public static boolean isValidSql(String expression) {
        if (expression == null) return false;
        String clean = expression.trim();
        if (clean.length() < 7) return false;
        return clean.toUpperCase().startsWith("SELECT ");
    }
}
