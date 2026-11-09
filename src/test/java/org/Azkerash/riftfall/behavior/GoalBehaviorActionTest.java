package org.Azkerash.riftfall.behavior;

import net.minecraft.world.entity.ai.goal.Goal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoalBehaviorActionTest {
    static class Executor extends Goal {
        int starts,ticks,stops,preparations,continuations;
        boolean ready=true,continuing=true;
        @Override public boolean canUse() { preparations++;return ready; }
        @Override public boolean canContinueToUse() { continuations++;return continuing; }
        @Override public void start() { starts++; }
        @Override public void tick() { ticks++; }
        @Override public void stop() { stops++; }
    }
    @Test void preparingNeverStartsOrTicksAnUnselectedExecutor() {
        var executor=new Executor();var action=new GoalBehaviorAction(executor);
        assertTrue(action.prepare());assertTrue(action.prepare());
        assertEquals(0,executor.starts);assertEquals(0,executor.ticks);assertFalse(action.running());
    }
    @Test void selectedActionUsesStartTickContinueStopLifecycleAndCanRestart() {
        var executor=new Executor();var action=new GoalBehaviorAction(executor);
        assertTrue(action.prepare());action.start(null);action.tick(null);action.tick(null);
        assertTrue(action.running());assertTrue(action.prepare());assertEquals(1,executor.continuations);
        executor.continuing=false;assertFalse(action.prepare());action.stop();action.stop();
        assertFalse(action.running());assertEquals(1,executor.starts);assertEquals(2,executor.ticks);assertEquals(1,executor.stops);
        assertTrue(action.prepare());action.start(null);assertEquals(2,executor.starts);
    }
}
