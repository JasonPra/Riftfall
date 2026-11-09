package org.Azkerash.riftfall.confidence;

import java.util.Map;

/** Data for a combat confidence model. Entity IDs and factor keys are extensible. */
public record ConfidenceProfile(Map<String,Double> unitWeights,Map<String,Double> factorWeights,double normalizationPoints) {
    public ConfidenceProfile {
        unitWeights=Map.copyOf(unitWeights);factorWeights=Map.copyOf(factorWeights);
        if (!Double.isFinite(normalizationPoints) || normalizationPoints<=0) throw new IllegalArgumentException("Confidence scale must be positive");
        unitWeights.forEach((key,value)-> {
            if (!Double.isFinite(value) || value<0) throw new IllegalArgumentException("Invalid unit weight: "+key);
        });
        factorWeights.forEach((key,value)-> {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid confidence factor: "+key);
        });
    }
}
