package org.Azkerash.riftfall.behavior;

import java.util.*;

/** Weighted utility selection. Profiles are reusable; current behavior belongs to each caller. */
public final class BehaviorEngine {
    public record Decision(Optional<String> selected,Map<String,Double> scores) {
        public Decision { scores=Map.copyOf(scores); }
    }
    private final List<BehaviorRule> rules;
    private final double switchMargin;
    public BehaviorEngine(List<BehaviorRule> rules,double switchMargin) {
        this.rules=List.copyOf(rules);this.switchMargin=switchMargin;
        if (!Double.isFinite(switchMargin) || switchMargin<0) throw new IllegalArgumentException("Invalid switch margin");
        if (rules.stream().map(BehaviorRule::id).distinct().count()!=rules.size()) throw new IllegalArgumentException("Duplicate behavior IDs");
    }
    public Set<String> behaviorIds() { return Set.copyOf(rules.stream().map(BehaviorRule::id).toList()); }
    public Decision select(BehaviorContext context,String current) {
        Map<String,Double> scores=new LinkedHashMap<>();String best=null;double highest=Double.NEGATIVE_INFINITY;
        for (var rule:rules) {
            if (!rule.eligible().test(context)) continue;
            double score=rule.score(context);
            if (!Double.isFinite(score)) throw new IllegalArgumentException("Non-finite behavior score: "+rule.id());
            scores.put(rule.id(),score);
            if (score>highest) { best=rule.id();highest=score; }
        }
        // Ineligible actions stop immediately. Eligible incumbents resist small score fluctuations.
        if (current!=null && scores.containsKey(current) && highest<=scores.get(current)+switchMargin) best=current;
        return new Decision(Optional.ofNullable(best),scores);
    }
}
