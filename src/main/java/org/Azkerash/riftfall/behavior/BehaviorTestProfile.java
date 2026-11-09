package org.Azkerash.riftfall.behavior;

import java.util.*;
import org.Azkerash.riftfall.confidence.*;

/** Demonstration policy, separate from the engine and from goblin behavior. */
public final class BehaviorTestProfile {
    public static ConfidenceConfig.Settings confidenceSettings() {
        var settings=ConfidenceConfig.settings();var weights=new HashMap<>(settings.profile().unitWeights());
        // A dedicated demo ally weight; an explicit server-config entry overrides it, including zero.
        weights.putIfAbsent("riftfall:behavior_tester",8D);
        return new ConfidenceConfig.Settings(new ConfidenceProfile(weights,settings.profile().factorWeights(),settings.profile().normalizationPoints()),
            settings.boldPoints(),settings.woundedFraction(),settings.vulnerableFraction(),settings.retreatingMultiplier());
    }
    public static BehaviorEngine engine() {
        return new BehaviorEngine(List.of(
            new BehaviorRule("idle",0,Map.of(),c->!c.active("target.visible") && !c.active("target.remembered")),
            new BehaviorRule("search",10,Map.of("confidence",5D),c->c.active("target.remembered")),
            new BehaviorRule("watch",5,Map.of("fear",30D,"self.wounded",50D),c->c.active("target.visible") && (c.active("self.wounded") || c.get("confidence")<.45) && c.get("target.distance")>=8),
            new BehaviorRule("approach",10,Map.of("confidence",20D),c->c.active("target.visible") && c.get("target.distance")>1.8),
            new BehaviorRule("attack",20,Map.of("confidence",20D),c->c.active("target.visible") && c.get("target.distance")<=1.8),
            new BehaviorRule("retreat",15,Map.of("fear",30D,"self.wounded",50D),
                c->c.active("target.visible") && (c.active("self.wounded") || c.get("confidence")<.45) && c.get("target.distance")<8)
        ),2);
    }
    private BehaviorTestProfile() {}
}
