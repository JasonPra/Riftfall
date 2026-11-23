package org.Azkerash.riftfall.behavior;

import static org.junit.jupiter.api.Assertions.*;

import org.Azkerash.riftfall.confidence.ConfidenceEngine;
import org.junit.jupiter.api.Test;

import java.util.*;

class WeightedBehaviorGoalTest {
    private static final class Observer implements BehaviorObserver {
        long now;
        int id, observations;
        boolean combat = true, urgent;
        String selected = "attack";

        @Override
        public long gameTime() {
            return now;
        }

        @Override
        public int observerId() {
            return id;
        }

        @Override
        public boolean engaged() {
            return combat;
        }

        @Override
        public boolean needsImmediateUpdate() {
            return urgent;
        }

        @Override
        public CombatSituation observe() {
            observations++;
            urgent = false;
            return new CombatSituation(
                    new BehaviorContext(Map.of("ready." + selected, 1D)),
                    new ConfidenceEngine.Result(20, .5, Map.of()),
                    null,
                    false,
                    null);
        }
    }

    private static final class Action implements BehaviorAction {
        int starts, ticks, stops;
        boolean continuing = true;

        @Override
        public void start(CombatSituation s) {
            starts++;
        }

        @Override
        public void tick(CombatSituation s) {
            ticks++;
        }

        @Override
        public void stop() {
            stops++;
        }

        @Override
        public boolean canContinue() {
            return continuing;
        }
    }

    private WeightedBehaviorGoal goal(Observer observer, Action attack, Action idle) {
        var engine =
                new BehaviorEngine(
                        List.of(
                                new BehaviorRule(
                                        "attack", 10, Map.of(), c -> c.active("ready.attack")),
                                new BehaviorRule("idle", 0, Map.of(), c -> true)),
                        0);
        return new WeightedBehaviorGoal(observer, engine, Map.of("attack", attack, "idle", idle));
    }

    @Test
    void expensiveDecisionsSlowDownWhileTheSelectedActionKeepsTicking() {
        var observer = new Observer();
        var action = new Action();
        var goal = goal(observer, action, new Action());
        for (int t = 0; t < 30; t++) {
            observer.now = t;
            goal.tick();
        }
        assertEquals(10, observer.observations);
        assertEquals(30, action.ticks);
        assertEquals(1, action.starts);
        assertEquals(0, action.stops);
    }

    @Test
    void idleDecisionsUseTheSlowerCadence() {
        var observer = new Observer();
        observer.combat = false;
        observer.selected = "idle";
        var idle = new Action();
        var goal = goal(observer, new Action(), idle);
        for (int t = 0; t < 30; t++) {
            observer.now = t;
            goal.tick();
        }
        assertEquals(3, observer.observations);
        assertEquals(30, idle.ticks);
    }

    @Test
    void urgentChangesInterruptBeforeAnotherExecutionTick() {
        var observer = new Observer();
        var attack = new Action();
        var idle = new Action();
        var goal = goal(observer, attack, idle);
        goal.tick();
        observer.now = 1;
        observer.selected = "idle";
        observer.urgent = true;
        goal.tick();
        assertEquals(2, observer.observations);
        assertEquals(1, attack.stops);
        assertEquals(1, attack.ticks);
        assertEquals(1, idle.ticks);
    }

    @Test
    void anUnsafeActiveExecutorForcesARecheckWithoutWaitingForTheClock() {
        var observer = new Observer();
        var attack = new Action();
        var idle = new Action();
        var goal = goal(observer, attack, idle);
        goal.tick();
        observer.now = 1;
        observer.selected = "idle";
        attack.continuing = false;
        goal.tick();
        assertEquals(2, observer.observations);
        assertEquals(1, attack.stops);
        assertEquals(1, attack.ticks);
    }

    @Test
    void stoppingClearsTheActionAndResumeAlwaysObservesAgain() {
        var observer = new Observer();
        var action = new Action();
        var goal = goal(observer, action, new Action());
        goal.tick();
        goal.stop();
        goal.stop();
        assertEquals(1, action.stops);
        observer.now = 1;
        goal.tick();
        assertEquals(2, observer.observations);
        assertEquals(2, action.starts);
    }
}
