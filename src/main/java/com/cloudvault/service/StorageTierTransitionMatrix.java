package com.cloudvault.service;

public final class StorageTierTransitionMatrix {
    public enum Tier {
        HOT(0),
        WARM(1),
        COLD(2),
        ARCHIVE(3),
        DEEP_ARCHIVE(4);

        private final int depth;
        Tier(int depth) { this.depth = depth; }
        public int getDepth() { return depth; }
    }

    private StorageTierTransitionMatrix() {}

    public static boolean canTransition(Tier from, Tier to) {
        if (from == null || to == null) return false;
        if (from == to) return true;
        return to.getDepth() > from.getDepth();
    }
}
