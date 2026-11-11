package org.Azkerash.riftfall.behavior;

import java.util.*;
import net.minecraft.world.entity.ai.goal.Goal;

/** Lifecycle bridge from the reusable utility engine into Minecraft's goal scheduler. */
public class WeightedBehaviorGoal extends Goal {
    private final CombatBehaviorController controller;
    private final BehaviorEngine engine;
    private final Map<String,BehaviorAction> actions;
    private String current;
    public WeightedBehaviorGoal(CombatBehaviorController controller,BehaviorEngine engine,Map<String,BehaviorAction> actions) {
        this.controller=Objects.requireNonNull(controller);this.engine=Objects.requireNonNull(engine);this.actions=Map.copyOf(actions);
        if (!this.actions.keySet().containsAll(engine.behaviorIds())) throw new IllegalArgumentException("Missing behavior action implementation");
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));
    }
    @Override public boolean canUse() { return true; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        var situation=controller.observe();String previous=current;
        String selected=engine.selectAction(situation.context(),previous);
        var diagnostic=controller.diagnosticsDue(selected)?engine.select(situation.context(),previous):null;
        if (!Objects.equals(current,selected)) {
            if (current!=null) actions.get(current).stop();
            current=selected;
            if (current!=null) actions.get(current).start(situation);
        }
        if (current!=null) actions.get(current).tick(situation);
        if(diagnostic!=null)controller.diagnostics(situation,diagnostic);
    }
    @Override public void stop() {
        if (current!=null) actions.get(current).stop();
        current=null;
    }
}
