package org.Azkerash.riftfall.behavior;

import java.util.*;

/** Weighted utility selection. Profiles are reusable; current behavior belongs to each caller. */
public final class BehaviorEngine {
    public record Evaluation(boolean eligible,double base,Map<String,Double> weights,Map<String,Double> terms,double total) {
        public Evaluation { weights=Map.copyOf(weights);terms=Map.copyOf(terms); }
    }
    public record Decision(Optional<String> selected,Map<String,Double> scores,Map<String,Evaluation> evaluations,double switchMargin,String previous) {
        public Decision { scores=Map.copyOf(scores);evaluations=Map.copyOf(evaluations); }
    }
    private final List<BehaviorRule> rules;
    private final double switchMargin;
    public BehaviorEngine(List<BehaviorRule> rules,double switchMargin) {
        this.rules=List.copyOf(rules);this.switchMargin=switchMargin;
        if (!Double.isFinite(switchMargin) || switchMargin<0) throw new IllegalArgumentException("Invalid switch margin");
        if (rules.stream().map(BehaviorRule::id).distinct().count()!=rules.size()) throw new IllegalArgumentException("Duplicate behavior IDs");
    }
    public Set<String> behaviorIds() { return Set.copyOf(rules.stream().map(BehaviorRule::id).toList()); }
    /** Allocation-free selection path; detailed score maps are only built for diagnostic snapshots. */
    public String selectAction(BehaviorContext context,String current) {
        String best=null;double highest=Double.NEGATIVE_INFINITY,incumbent=Double.NEGATIVE_INFINITY;boolean currentEligible=false;
        for(var rule:rules){
            if(!rule.eligible().test(context))continue;
            double score=rule.score(context);
            if(!Double.isFinite(score))throw new IllegalArgumentException("Non-finite behavior score: "+rule.id());
            if(rule.id().equals(current)){incumbent=score;currentEligible=true;}
            if(score>highest){best=rule.id();highest=score;}
        }
        return currentEligible && highest<=incumbent+switchMargin?current:best;
    }
    public Decision select(BehaviorContext context,String current) {
        Map<String,Double> scores=new LinkedHashMap<>();Map<String,Evaluation> evaluations=new LinkedHashMap<>();String best=null;double highest=Double.NEGATIVE_INFINITY;
        for (var rule:rules) {
            boolean eligible=rule.eligible().test(context);
            Map<String,Double> terms=new LinkedHashMap<>();rule.weights().forEach((key,weight)->terms.put(key,weight*context.get(key)));
            double score=rule.score(context);
            if (!Double.isFinite(score)) throw new IllegalArgumentException("Non-finite behavior score: "+rule.id());
            evaluations.put(rule.id(),new Evaluation(eligible,rule.base(),rule.weights(),terms,score));
            if (!eligible) continue;
            scores.put(rule.id(),score);
            if (score>highest) { best=rule.id();highest=score; }
        }
        // Ineligible actions stop immediately. Eligible incumbents resist small score fluctuations.
        if (current!=null && scores.containsKey(current) && highest<=scores.get(current)+switchMargin) best=current;
        return new Decision(Optional.ofNullable(best),scores,evaluations,switchMargin,current);
    }
}
