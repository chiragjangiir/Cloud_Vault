package com.cloudvault.service;

public final class StorageClassTransitionCostComparator {
    public enum TierRank {
        STANDARD(1),
        STANDARD_IA(2),
        ONEZONE_IA(3),
        GLACIER(4),
        DEEP_ARCHIVE(5);

        private final int rank;
        TierRank(int rank) { this.rank = rank; }
        public int getRank() { return rank; }
    }

    private StorageClassTransitionCostComparator() {}

    public static boolean isValidDownwardTransition(TierRank from, TierRank to) {
        if (from == null || to == null) return false;
        return to.getRank() > from.getRank();
    }
}
