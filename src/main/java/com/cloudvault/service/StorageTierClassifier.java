package com.cloudvault.service;

public class StorageTierClassifier {
    public enum Tier { HOT, WARM, COLD, ARCHIVE }

    public static Tier classify(long daysSinceLastAccess, long sizeBytes) {
        if (daysSinceLastAccess < 30) {
            return Tier.HOT;
        } else if (daysSinceLastAccess < 90) {
            return Tier.WARM;
        } else if (daysSinceLastAccess < 365) {
            return Tier.COLD;
        } else {
            return Tier.ARCHIVE;
        }
    }
}
