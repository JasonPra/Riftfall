package org.Azkerash.riftfall.fauna;
import java.util.*;
import org.Azkerash.riftfall.behavior.*;
/** Pure policy; shared utility selection remains separate from world actions. */
final class RyBehaviorProfile {
    static boolean shouldRetreat(double health,double points,boolean pup,boolean threat) {
        return threat&&(pup||health<=.22||health<.45&&points<-10);
    }
    static BehaviorEngine engine() {
        var r=new ArrayList<BehaviorRule>();
        r.add(new BehaviorRule("interrupted",3000,Map.of(),c->c.active("interrupted")));
        for(var entry:Map.ofEntries(Map.entry("retreat",1000),Map.entry("cast",600),Map.entry("bite",550),
            Map.entry("guard",350),Map.entry("regroup",300),Map.entry("flank",250),Map.entry("pursue",200),
            Map.entry("search",150),Map.entry("warn",130),Map.entry("rest",100),Map.entry("patrol",10)).entrySet())
            r.add(new BehaviorRule(entry.getKey(),entry.getValue(),Map.of("committed."+entry.getKey(),1200D),c->c.active("ready."+entry.getKey())));
        r.add(new BehaviorRule("idle",0,Map.of(),c->true));return new BehaviorEngine(r,4);
    }
    private RyBehaviorProfile() {}
}
