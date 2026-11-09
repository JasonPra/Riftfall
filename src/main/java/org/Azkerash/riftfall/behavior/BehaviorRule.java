package org.Azkerash.riftfall.behavior;

import java.util.Map;
import java.util.function.Predicate;

/** Eligibility is a requirement; weights express preference among eligible actions. */
public record BehaviorRule(String id,double base,Map<String,Double> weights,Predicate<BehaviorContext> eligible) {
    public BehaviorRule {
        if (id.isBlank() || !Double.isFinite(base)) throw new IllegalArgumentException("Invalid behavior rule");
        weights=Map.copyOf(weights);java.util.Objects.requireNonNull(eligible);
        weights.forEach((key,value)-> {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid weight: "+key);
        });
    }
    public double score(BehaviorContext context) {
        return base+weights.entrySet().stream().mapToDouble(e->e.getValue()*context.get(e.getKey())).sum();
    }
}
