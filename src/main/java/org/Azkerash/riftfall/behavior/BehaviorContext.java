package org.Azkerash.riftfall.behavior;

import java.util.Map;

/** Immutable, named situational inputs. Missing signals have value zero. No entity dependencies. */
public record BehaviorContext(Map<String,Double> signals) {
    public BehaviorContext {
        signals=Map.copyOf(signals);
        signals.forEach((key,value)-> {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite signal: "+key);
        });
    }
    public double get(String key) { Double value=signals.get(key);return value==null?0:value; }
    public boolean active(String key) { return get(key)>0; }
}
