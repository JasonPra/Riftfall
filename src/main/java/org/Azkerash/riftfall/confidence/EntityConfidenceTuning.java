package org.Azkerash.riftfall.confidence;

import java.util.*;

/** Per-entity-type overlays. A supplied species profile remains the base; only explicit keys are replaced. */
public final class EntityConfidenceTuning {
    public static boolean valid(String key,double value) {
        if (!Double.isFinite(value)) return false;
        if (key.startsWith("factor.")) return key.substring(7).matches("[a-z0-9_.:-]+") && value>=-1000 && value<=1000;
        if (key.startsWith("unit.")) return key.substring(5).matches("[a-z0-9_.-]+:[a-z0-9/._-]+") && value>=0 && value<=1000;
        if (org.Azkerash.riftfall.behavior.PanicSettings.validOverride(key,value)) return true;
        return switch (key) {
            case "normalizationPoints" -> value>=1 && value<=10000;
            case "healingEntryHealthFraction","healingExitHealthFraction" -> value>=0 && value<=1;
            case "healingSearchRadius" -> value>=1 && value<=64;
            case "healingDangerDistance" -> value>=0 && value<=32;
            case "healingRiskPoints" -> value>=-1000 && value<=1000;
            case "engagePoints" -> value>=-1000 && value<=1000;
            case "boldPoints" -> value>=0 && value<=10000;
            case "woundedHealthFraction","vulnerableTargetHealthFraction","retreatingAllyMultiplier" -> value>=0 && value<=1;
            case "witnessRadius" -> value>=0 && value<=128;
            case "witnessDurationTicks" -> value>=0 && value<=72000 && value==Math.rint(value);
            case "witnessMaxStacks" -> value>=0 && value<=32 && value==Math.rint(value);
            default -> false;
        };
    }
    public static ConfidenceConfig.Settings apply(ConfidenceConfig.Settings base,Map<String,Double> overrides) {
        overrides.forEach((key,value)-> { if (!valid(key,value)) throw new IllegalArgumentException("Invalid confidence override: "+key); });
        var units=new HashMap<>(base.profile().unitWeights());var factors=new HashMap<>(base.profile().factorWeights());
        overrides.forEach((key,value)-> {
            if (key.startsWith("unit.")) units.put(key.substring(5),value);
            if (key.startsWith("factor.")) factors.put(key.substring(7),value);
        });
        return new ConfidenceConfig.Settings(new ConfidenceProfile(units,factors,overrides.getOrDefault("normalizationPoints",base.profile().normalizationPoints())),
            overrides.getOrDefault("boldPoints",base.boldPoints()),overrides.getOrDefault("woundedHealthFraction",base.woundedFraction()),
            overrides.getOrDefault("vulnerableTargetHealthFraction",base.vulnerableFraction()),overrides.getOrDefault("retreatingAllyMultiplier",base.retreatingMultiplier()));
    }
    public static WitnessSettings witness(WitnessSettings base,Map<String,Double> overrides) {
        overrides.forEach((key,value)-> { if (!valid(key,value)) throw new IllegalArgumentException("Invalid confidence override: "+key); });
        return new WitnessSettings(overrides.getOrDefault("witnessRadius",base.radius()),
            overrides.getOrDefault("witnessDurationTicks",(double)base.durationTicks()).intValue(),
            overrides.getOrDefault("witnessMaxStacks",(double)base.maxStacks()).intValue());
    }
    private EntityConfidenceTuning() {}
}
