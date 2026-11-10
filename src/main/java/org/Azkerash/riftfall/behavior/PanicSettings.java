package org.Azkerash.riftfall.behavior;

import java.util.Map;

/** Raw confidence points, game ticks and blocks. Species can supply independent settings. */
public record PanicSettings(double entryPoints,double recoveryPoints,int entryTicks,int recoveryTicks,
        int minimumTicks,double escapeDistance,double interruptDistance,int burrowTicks,double fleeSpeed) {
    public PanicSettings {
        if(!Double.isFinite(entryPoints)||!Double.isFinite(recoveryPoints)||recoveryPoints<=entryPoints
                ||entryTicks<1||recoveryTicks<1||minimumTicks<0||!Double.isFinite(escapeDistance)||escapeDistance<4||escapeDistance>32
                ||!Double.isFinite(interruptDistance)||interruptDistance<1||interruptDistance>=escapeDistance
                ||burrowTicks<10||burrowTicks>200||!Double.isFinite(fleeSpeed)||fleeSpeed<1||fleeSpeed>3)
            throw new IllegalArgumentException("Invalid panic settings");
    }
    public static PanicSettings defaults(){return new PanicSettings(-15,5,20,60,100,12,3,50,1.5);}
    public PanicSettings overrides(Map<String,Double> v){
        double entry=v.getOrDefault("panicEntryPoints",entryPoints),recovery=v.getOrDefault("panicRecoveryPoints",recoveryPoints);
        return new PanicSettings(entry,Math.max(entry+.01,recovery),
            v.getOrDefault("panicEntryTicks",(double)entryTicks).intValue(),v.getOrDefault("panicRecoveryTicks",(double)recoveryTicks).intValue(),
            v.getOrDefault("panicMinimumTicks",(double)minimumTicks).intValue(),v.getOrDefault("panicEscapeDistance",escapeDistance),
            Math.min(v.getOrDefault("panicBurrowDangerDistance",interruptDistance),v.getOrDefault("panicEscapeDistance",escapeDistance)-.01),
            v.getOrDefault("panicBurrowTicks",(double)burrowTicks).intValue(),v.getOrDefault("panicFleeSpeed",fleeSpeed));
    }
    public static boolean validOverride(String key,double value){
        if(!Double.isFinite(value))return false;
        return switch(key){
            case "panicEntryPoints","panicRecoveryPoints" -> value>=-1000&&value<=1000;
            case "panicEntryTicks","panicRecoveryTicks" -> value>=1&&value<=1200&&value==Math.rint(value);
            case "panicMinimumTicks" -> value>=0&&value<=2400&&value==Math.rint(value);
            case "panicBurrowDangerDistance" -> value>=1&&value<=31;
            case "panicEscapeDistance" -> value>=4&&value<=32;
            case "panicBurrowTicks" -> value>=10&&value<=200&&value==Math.rint(value);
            case "panicFleeSpeed" -> value>=1&&value<=3;
            default -> false;
        };
    }
}
