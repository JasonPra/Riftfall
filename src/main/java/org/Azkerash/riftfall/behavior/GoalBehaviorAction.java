package org.Azkerash.riftfall.behavior;

import net.minecraft.world.entity.ai.goal.Goal;
import java.util.Objects;

/** Adapts an existing movement/attack executor to utility-controlled lifecycle, without registering it in a selector. */
public final class GoalBehaviorAction implements BehaviorAction {
    private final Goal executor;
    private boolean running;
    public GoalBehaviorAction(Goal executor) { this.executor=Objects.requireNonNull(executor); }
    /** Called once per decision update, not from rule predicates (some executors prepare paths here). */
    public boolean prepare() { return running?executor.canContinueToUse():executor.canUse(); }
    public boolean running() { return running; }
    @Override public void start(CombatSituation situation) { executor.start();running=true; }
    @Override public void tick(CombatSituation situation) { executor.tick(); }
    @Override public void stop() { if (running) executor.stop();running=false; }
}
