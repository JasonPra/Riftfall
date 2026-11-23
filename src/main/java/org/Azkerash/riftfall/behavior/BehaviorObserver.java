package org.Azkerash.riftfall.behavior;

/** Observation port for the scheduler: cheap invalidation is separate from expensive sensing. */
public interface BehaviorObserver {
    long gameTime();

    int observerId();

    boolean engaged();

    boolean needsImmediateUpdate();

    CombatSituation observe();

    default boolean diagnosticsDue(String action) {
        return false;
    }

    default void diagnostics(CombatSituation situation, BehaviorEngine.Decision decision) {}
}
