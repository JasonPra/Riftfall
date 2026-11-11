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
        double sum=0,correction=0,simple=0;
        // Match DoubleStream.sum's compensated arithmetic without allocating a stream per rule.
        for(var entry:weights.entrySet()){
            double value=entry.getValue()*context.get(entry.getKey());simple+=value;
            double adjusted=value-correction,next=sum+adjusted;correction=(next-sum)-adjusted;sum=next;
        }
        double total=sum-correction;
        return base+(Double.isNaN(total)&&Double.isInfinite(simple)?simple:total);
    }
}
