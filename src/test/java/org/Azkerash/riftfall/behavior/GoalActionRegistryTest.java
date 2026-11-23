package org.Azkerash.riftfall.behavior;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.entity.ai.goal.Goal;

import org.junit.jupiter.api.Test;

import java.util.*;

class GoalActionRegistryTest {
    private static final class Probe extends Goal {
        int starts, ticks, preparations, continuations;
        boolean ready, continuing;

        Probe(boolean ready, boolean continuing) {
            this.ready = ready;
            this.continuing = continuing;
        }

        @Override
        public boolean canUse() {
            preparations++;
            return ready;
        }

        @Override
        public boolean canContinueToUse() {
            continuations++;
            return continuing;
        }

        @Override
        public void start() {
            starts++;
        }

        @Override
        public void tick() {
            ticks++;
        }
    }

    @Test
    void cachesBothPositiveAndNegativePreparationOnlyWithinOneDecision() {
        var registry = new GoalActionRegistry();
        var probe = new Probe(false, true);
        registry.register("path", probe, () -> false);
        var entry = registry.entries().getFirst();
        registry.beginDecision();
        assertFalse(entry.prepare());
        probe.ready = true;
        assertFalse(entry.prepare());
        assertEquals(1, probe.preparations);
        assertEquals(0, probe.starts);
        assertEquals(0, probe.ticks);
        registry.beginDecision();
        assertTrue(entry.prepare());
        assertTrue(entry.prepare());
        assertEquals(2, probe.preparations);
    }

    @Test
    void aFailedContinuationIsNotPreparedAgainWhenResolvingEligibility() {
        var registry = new GoalActionRegistry();
        var first = new Probe(true, false);
        var second = new Probe(true, true);
        registry.register("first", first, () -> true);
        registry.register("second", second, () -> true);
        registry.action("first").start(null);
        registry.action("second").start(null);
        registry.beginDecision();
        assertEquals("second", registry.firstContinuingCommitment().id());
        assertFalse(registry.entries().getFirst().prepare());
        assertEquals(1, first.continuations);
        assertEquals(1, second.continuations);
    }

    @Test
    void commitmentsUseRegistrationOrderAndDoNotPrepareUnrelatedGoals() {
        var registry = new GoalActionRegistry();
        var first = new Probe(true, true);
        var second = new Probe(true, true);
        registry.register("first", first, () -> true);
        registry.register("second", second, () -> true);
        registry.beginDecision();
        assertFalse(registry.anyCommitted(List.of("missing", "first")));
        registry.action("first").start(null);
        registry.action("second").start(null);
        assertTrue(registry.anyCommitted(List.of("first")));
        assertEquals(0, first.continuations);
        assertEquals("first", registry.firstContinuingCommitment().id());
        assertEquals(0, second.continuations);
    }

    @Test
    void stoppedGoalsReenterThroughCanUseOnTheNextDecision() {
        var registry = new GoalActionRegistry();
        var probe = new Probe(true, true);
        registry.register("attack", probe, () -> true);
        registry.action("attack").start(null);
        registry.beginDecision();
        assertNotNull(registry.firstContinuingCommitment());
        registry.action("attack").stop();
        registry.beginDecision();
        assertNull(registry.firstContinuingCommitment());
        assertTrue(registry.entries().getFirst().prepare());
        assertEquals(1, probe.continuations);
        assertEquals(1, probe.preparations);
    }

    @Test
    void bindingsCannotBeOverwrittenAndSnapshotsCannotMutateOwnedBindings() {
        var registry = new GoalActionRegistry();
        registry.register("idle", new Probe(true, true), () -> false);
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register("idle", new Probe(false, false), () -> true));
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(" ", new Probe(true, true), () -> false));
        assertThrows(IllegalArgumentException.class, () -> registry.action("missing"));
        assertThrows(UnsupportedOperationException.class, () -> registry.entries().clear());
        registry.actions().clear();
        assertEquals(1, registry.entries().size());
        assertNotNull(registry.action("idle"));
    }

    @Test
    void signalNamesAndCommitmentReflectEligibilityAndRunningState() {
        var registry = new GoalActionRegistry();
        registry.register("attack", new Probe(true, true), () -> true);
        var entry = registry.entries().getFirst();
        var signals = new HashMap<String, Double>();
        entry.publish(signals, true);
        assertEquals(Map.of("ready.attack", 1D, "committed.attack", 0D), signals);
        entry.action().start(null);
        entry.publish(signals, true);
        assertEquals(1D, signals.get("committed.attack"));
        entry.publish(signals, false);
        assertEquals(Map.of("ready.attack", 0D, "committed.attack", 0D), signals);
    }

    @Test
    void matchesPreviousPreparationOrderResultsAndCallCountsAcrossRandomSituations() {
        var random = new Random(14723);
        for (int sample = 0; sample < 2000; sample++) {
            var registry = new GoalActionRegistry();
            var oldActions = new LinkedHashMap<String, GoalBehaviorAction>();
            var oldProbes = new ArrayList<Probe>();
            var newProbes = new ArrayList<Probe>();
            var commitments = new HashMap<String, Boolean>();
            var allowed = new HashMap<String, Boolean>();
            int count = 1 + random.nextInt(20);
            boolean interrupted = random.nextBoolean();
            for (int i = 0; i < count; i++) {
                String id = "action" + i;
                boolean ready = random.nextBoolean(),
                        continuing = random.nextBoolean(),
                        running = random.nextBoolean(),
                        committed = random.nextBoolean();
                var oldProbe = new Probe(ready, continuing);
                var newProbe = new Probe(ready, continuing);
                oldProbes.add(oldProbe);
                newProbes.add(newProbe);
                commitments.put(id, committed);
                allowed.put(id, random.nextBoolean());
                var oldAction = new GoalBehaviorAction(oldProbe);
                oldActions.put(id, oldAction);
                registry.register(id, newProbe, () -> committed);
                if (running) {
                    oldAction.start(null);
                    registry.action(id).start(null);
                }
            }
            var prepared = new HashMap<String, Boolean>();
            String oldCommitment = null;
            if (!interrupted) {
                for (var entry : oldActions.entrySet()) {
                    if (entry.getValue().running() && commitments.get(entry.getKey())) {
                        boolean continuing = entry.getValue().prepare();
                        prepared.put(entry.getKey(), continuing);
                        if (continuing) {
                            oldCommitment = entry.getKey();
                            break;
                        }
                    }
                }
            }
            var expected = new LinkedHashMap<String, Double>();
            for (var entry : oldActions.entrySet()) {
                String id = entry.getKey();
                var action = entry.getValue();
                boolean ready =
                        !interrupted
                                && allowed.get(id)
                                && (oldCommitment == null || oldCommitment.equals(id))
                                && (prepared.containsKey(id) ? prepared.get(id) : action.prepare());
                expected.put("ready." + id, ready ? 1D : 0D);
                expected.put(
                        "committed." + id,
                        ready && action.running() && commitments.get(id) ? 1D : 0D);
            }
            registry.beginDecision();
            var current = interrupted ? null : registry.firstContinuingCommitment();
            var actual = new LinkedHashMap<String, Double>();
            for (var entry : registry.entries()) {
                entry.publish(
                        actual,
                        !interrupted
                                && allowed.get(entry.id())
                                && (current == null || current == entry)
                                && entry.prepare());
            }
            assertEquals(expected, actual, "sample " + sample);
            for (int i = 0; i < count; i++) {
                assertEquals(oldProbes.get(i).preparations, newProbes.get(i).preparations);
                assertEquals(oldProbes.get(i).continuations, newProbes.get(i).continuations);
            }
        }
    }
}
