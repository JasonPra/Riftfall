package org.Azkerash.riftfall.behavior;

/** Multi-rate decision clock. Execution ticks and urgent invalidation are handled separately. */
public final class BehaviorDecisionSchedule {
    public static final int DEFAULT_COMBAT_INTERVAL = 3;
    public static final int DEFAULT_IDLE_INTERVAL = 10;
    private final int combatInterval;
    private final int idleInterval;
    private long nextDecision = Long.MIN_VALUE;

    public BehaviorDecisionSchedule() {
        this(DEFAULT_COMBAT_INTERVAL, DEFAULT_IDLE_INTERVAL);
    }

    public BehaviorDecisionSchedule(int combatInterval, int idleInterval) {
        if (combatInterval < 1 || idleInterval < combatInterval) {
            throw new IllegalArgumentException("Require 1 <= combat interval <= idle interval");
        }
        this.combatInterval = combatInterval;
        this.idleInterval = idleInterval;
    }

    public boolean due(long now) {
        return now >= nextDecision;
    }

    public void completed(long now, int entityId, boolean engaged) {
        nextDecision = SensingSchedule.next(now, engaged ? combatInterval : idleInterval, entityId);
    }

    public void reset() {
        nextDecision = Long.MIN_VALUE;
    }
}
