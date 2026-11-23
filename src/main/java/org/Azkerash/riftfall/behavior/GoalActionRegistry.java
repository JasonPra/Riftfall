package org.Azkerash.riftfall.behavior;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Per-entity goal bindings and preparation cache. Registration order defines commitment priority.
 * Call beginDecision before sensing an update; preparing an entry never starts its executor. Entity
 * AI owns this registry on the server thread.
 */
public final class GoalActionRegistry {
    private final Map<String, Entry> byId = new LinkedHashMap<>();
    private final List<Entry> ordered = new ArrayList<>();
    private final List<Entry> view = Collections.unmodifiableList(ordered);

    public void register(String id, Goal executor, BooleanSupplier commitment) {
        Objects.requireNonNull(id, "Action ID");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Blank action ID");
        }
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate action ID: " + id);
        }
        Entry entry = new Entry(id, executor, commitment);
        byId.put(id, entry);
        ordered.add(entry);
    }

    public List<Entry> entries() {
        return view;
    }

    public GoalBehaviorAction action(String id) {
        Entry entry = byId.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown action ID: " + id);
        }
        return entry.action;
    }

    public void beginDecision() {
        for (Entry entry : ordered) {
            entry.prepared = false;
        }
    }

    /** Does not call canUse/canContinueToUse or otherwise prepare goals. */
    public boolean anyCommitted(Collection<String> ids) {
        for (String id : ids) {
            Entry entry = byId.get(id);
            if (entry != null && entry.committed()) {
                return true;
            }
        }
        return false;
    }

    /** Keep a continuing committed action ahead of newly eligible alternatives. */
    public Entry firstContinuingCommitment() {
        for (Entry entry : ordered) {
            if (entry.committed() && entry.prepare()) {
                return entry;
            }
        }
        return null;
    }

    /** Initial goal wiring can wrap actions without changing the owned executor bindings. */
    public Map<String, BehaviorAction> actions() {
        Map<String, BehaviorAction> result = new LinkedHashMap<>();
        for (Entry entry : ordered) {
            result.put(entry.id, entry.action);
        }
        return result;
    }

    public static final class Entry {
        private static final Double ONE = 1D, ZERO = 0D;
        private final String id;
        private final String readyKey;
        private final String committedKey;
        private final GoalBehaviorAction action;
        private final BooleanSupplier commitment;
        private boolean prepared;
        private boolean ready;

        private Entry(String id, Goal executor, BooleanSupplier commitment) {
            this.id = id;
            this.readyKey = "ready." + id;
            this.committedKey = "committed." + id;
            this.action = new GoalBehaviorAction(executor);
            this.commitment = Objects.requireNonNull(commitment, "Commitment supplier");
        }

        public String id() {
            return id;
        }

        public GoalBehaviorAction action() {
            return action;
        }

        public boolean committed() {
            return action.running() && commitment.getAsBoolean();
        }

        /** A false result is cached too: failed path preparation must not run twice this tick. */
        public boolean prepare() {
            if (!prepared) {
                ready = action.prepare();
                prepared = true;
            }
            return ready;
        }

        public void publish(Map<String, Double> signals, boolean eligible) {
            signals.put(readyKey, eligible ? ONE : ZERO);
            signals.put(committedKey, eligible && committed() ? ONE : ZERO);
        }
    }
}
