package org.Azkerash.riftfall.behavior;

/** Per-observer tuning; confidence uses raw points, health uses fractions. */
public record HealingRetreatSettings(double entryHealth,double exitHealth,double searchRadius,double dangerDistance,double riskPoints) {
    public HealingRetreatSettings {
        if (!Double.isFinite(entryHealth) || entryHealth<0 || entryHealth>1 || !Double.isFinite(exitHealth) || exitHealth<0 || exitHealth>1
                || !Double.isFinite(searchRadius) || searchRadius<1 || searchRadius>64 || !Double.isFinite(dangerDistance) || dangerDistance<0 || dangerDistance>32
                || !Double.isFinite(riskPoints) || riskPoints< -1000 || riskPoints>1000) throw new IllegalArgumentException("Invalid healing retreat settings");
    }
    public static HealingRetreatSettings defaults() { return new HealingRetreatSettings(.35,.8,24,5,25); }
}
