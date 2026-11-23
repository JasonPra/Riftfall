package org.Azkerash.riftfall.behavior;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.*;

/** Multi-rate utility decisions with tick-accurate execution of the selected action. */
public class WeightedBehaviorGoal extends Goal {
    private final BehaviorObserver controller;
    private final BehaviorEngine engine;
    private final Map<String, BehaviorAction> actions;
    private final BehaviorDecisionSchedule schedule = new BehaviorDecisionSchedule();
    private CombatSituation situation;
    private String current;

    public WeightedBehaviorGoal(
            BehaviorObserver controller,
            BehaviorEngine engine,
            Map<String, BehaviorAction> actions) {
        this.controller = Objects.requireNonNull(controller);
        this.engine = Objects.requireNonNull(engine);
        this.actions = Map.copyOf(actions);
        if (!this.actions.keySet().containsAll(engine.behaviorIds())) {
            throw new IllegalArgumentException("Missing behavior action implementation");
        }
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        long now = controller.gameTime();
        boolean decide = schedule.due(now) || controller.needsImmediateUpdate();
        // Only the active executor is checked between decisions. Finished/unsafe actions stop
        // before the next impact tick, without preparing every alternative or scanning allies.
        if (!decide && current != null && !actions.get(current).canContinue()) {
            decide = true;
        }
        if (decide) {
            situation = controller.observe();
            String previous = current;
            String selected = engine.selectAction(situation.context(), previous);
            var diagnostic =
                    controller.diagnosticsDue(selected)
                            ? engine.select(situation.context(), previous)
                            : null;
            if (!Objects.equals(current, selected)) {
                if (current != null) {
                    actions.get(current).stop();
                }
                current = selected;
                if (current != null) {
                    actions.get(current).start(situation);
                }
            }
            if (diagnostic != null) {
                controller.diagnostics(situation, diagnostic);
            }
            schedule.completed(now, controller.observerId(), controller.engaged());
        }
        if (current != null) {
            actions.get(current).tick(situation);
        }
    }

    @Override
    public void stop() {
        if (current != null) {
            actions.get(current).stop();
        }
        current = null;
        situation = null;
        schedule.reset();
    }
}
