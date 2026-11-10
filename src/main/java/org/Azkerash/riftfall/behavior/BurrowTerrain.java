package org.Azkerash.riftfall.behavior;

/** Soft material alone is insufficient: the block must also belong to the natural-soil allowlist. */
public final class BurrowTerrain {
    public static final float MAX_HARDNESS=.8F;
    public static boolean canDig(boolean naturalSoil,float hardness){
        return naturalSoil&&Float.isFinite(hardness)&&hardness>=0&&hardness<=MAX_HARDNESS;
    }
    private BurrowTerrain() {}
}
