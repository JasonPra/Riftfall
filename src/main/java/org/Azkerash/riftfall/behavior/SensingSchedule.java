package org.Azkerash.riftfall.behavior;
/** Spread periodic work by entity ID while preserving the maximum sensing interval. */
public final class SensingSchedule {
    public static long next(long now,int interval,int entityId){
        if(interval<1)throw new IllegalArgumentException("Invalid sensing interval");
        return now+interval-Math.floorMod(now+(long)entityId,interval);
    }
    private SensingSchedule(){}
}
