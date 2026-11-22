package com.cloudvault.service;

public final class PartNumberValidator {
    public static final int MIN_PART_NUMBER = 1;
    public static final int MAX_PART_NUMBER = 10000;

    private PartNumberValidator() {}

    public static boolean isValidPartNumber(int partNumber) {
        return partNumber >= MIN_PART_NUMBER && partNumber <= MAX_PART_NUMBER;
    }
}
