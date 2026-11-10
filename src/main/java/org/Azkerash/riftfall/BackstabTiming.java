package org.Azkerash.riftfall;
/** Shared timeline math: a lethal finisher cannot resolve before either participant finishes. */
public final class BackstabTiming {
    public static long fatalResolution(long attackerUntil,long impact,int fallTicks){return Math.max(attackerUntil,impact+fallTicks);}
    public static float initialLethalDamage(float health){return Math.max(0,health-1);}
    /** Includes the recovery clip's .08-second link; match the first upward motion in the authored clips. */
    public static int getupDelayTicks(boolean greatsword){return greatsword?50:42;}
    public static boolean vulnerable(boolean fallen,boolean fatalPending,long now,long getupAt){return fallen && !fatalPending && now>=getupAt;}
    private BackstabTiming(){}
}
