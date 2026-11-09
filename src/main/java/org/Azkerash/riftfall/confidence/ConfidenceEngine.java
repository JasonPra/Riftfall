package org.Azkerash.riftfall.confidence;

import java.util.LinkedHashMap;
import java.util.Map;

/** Mob-independent combat scoring. Collection, faction rules and behavior decisions belong to callers. */
public final class ConfidenceEngine {
    public record Result(double points,double value,Map<String,Double> contributions) {
        public Result { contributions=Map.copyOf(contributions); }
        public boolean atLeast(double threshold) { return points>=threshold; }
    }
    /** Unit counts can be fractional, e.g. a retreating ally contributing 0.25 of its normal weight. */
    public static Result evaluate(ConfidenceProfile profile,Map<String,Double> nearbyUnits,Map<String,Double> factors) {
        Map<String,Double> contributions=new LinkedHashMap<>();
        nearbyUnits.forEach((id,count)-> {
            if (!Double.isFinite(count) || count<0) throw new IllegalArgumentException("Invalid nearby unit count: "+id);
            double points=count*profile.unitWeights().getOrDefault(id,0D);
            if (points!=0) contributions.put("allies."+id,points);
        });
        factors.forEach((key,value)-> {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid factor input: "+key);
            double points=value*profile.factorWeights().getOrDefault(key,0D);
            if (points!=0) contributions.put("factor."+key,points);
        });
        double total=contributions.values().stream().mapToDouble(Double::doubleValue).sum();
        return new Result(total,Math.clamp(total/profile.normalizationPoints(),0,1),contributions);
    }
    private ConfidenceEngine() {}
}
