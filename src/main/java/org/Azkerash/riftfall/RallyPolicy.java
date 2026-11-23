package org.Azkerash.riftfall;

/** Pure morale trigger and resource-aware single/group choice. */
final class RallyPolicy {
    static boolean atRisk(double confidencePoints, double panicEntryPoints) {
        return confidencePoints <= panicEntryPoints + 15;
    }

    static int level(int needyAllies, double mana, int singleCost, int groupCost, int maxLevel) {
        if (needyAllies <= 0 || mana < singleCost || maxLevel < 1) {
            return 0;
        }
        return needyAllies >= 2 && maxLevel >= 2 && mana >= groupCost ? 2 : 1;
    }

    private RallyPolicy() {}
}
