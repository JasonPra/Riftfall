package org.Azkerash.riftfall;

import java.util.*;
import org.Azkerash.riftfall.behavior.*;

/** Goblin preferences, separate from the shared engine and the movement/animation executors. */
final class GoblinBehaviorProfile {
    static BehaviorEngine engine() {
        var rules=new ArrayList<BehaviorRule>();
        rules.add(new BehaviorRule("interrupted",5000,Map.of(),c->c.active("self.interrupted")));
        rules.add(rule("panic_escape",2000,Map.of("fear",100D)));
        rules.add(rule("seek_healing",700,Map.of("fear",50D,"self.wounded",50D)));
        rules.add(rule("warrior_retreat",500,Map.of("fear",50D,"self.wounded",40D)));
        rules.add(rule("cautious_retreat",300,Map.of("fear",50D)));
        rules.add(new BehaviorRule("watch",100,Map.of("fear",20D),c->c.active("self.melee_role") && c.active("target.visible") && !c.active("engage")));
        rules.add(rule("grapple",400,Map.of("confidence",20D)));
        rules.add(rule("plunge",350,Map.of("confidence",20D)));
        rules.add(rule("staff",500,Map.of("target.exposed",40D)));
        rules.add(rule("heal",400,Map.of("fear",10D)));
        rules.add(rule("ranged_retreat",300,Map.of("fear",50D)));
        rules.add(rule("cast",250,Map.of("confidence",20D)));
        rules.add(rule("position",160,Map.of("fear",10D)));
        rules.add(rule("bow",140,Map.of("confidence",10D,"cover.safe",200D)));
        rules.add(rule("melee",150,Map.of("confidence",20D)));
        rules.add(new BehaviorRule("search",50,Map.of("confidence",5D),c->c.active("target.remembered") && (!c.active("self.melee_role") || c.active("engage"))));
        rules.add(rule("wander",10,Map.of()));
        rules.add(new BehaviorRule("idle",0,Map.of(),c->true));
        return new BehaviorEngine(rules,2);
    }
    private static BehaviorRule rule(String id,double base,Map<String,Double> preferences) {
        var weights=new LinkedHashMap<>(preferences);weights.put("committed."+id,1000D);
        return new BehaviorRule(id,base,weights,c->c.active("ready."+id));
    }
    private GoblinBehaviorProfile() {}
}
