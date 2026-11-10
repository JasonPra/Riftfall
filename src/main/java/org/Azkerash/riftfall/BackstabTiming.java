package org.Azkerash.riftfall;
/** Shared timeline math: a lethal finisher cannot resolve before either participant finishes. */
public final class BackstabTiming {
    public static long fatalResolution(long attackerUntil,long impact,int fallTicks){return Math.max(attackerUntil,impact+fallTicks);}
    public static float initialLethalDamage(float health){return Math.max(0,health-1);}
    private BackstabTiming(){}
}
