package org.Azkerash.riftfall.behavior;

/** Per-entity action callbacks; tick executes only while selected. */
@FunctionalInterface
public interface BehaviorAction {
    void tick(CombatSituation situation);

    default void start(CombatSituation situation) {}

    default void stop() {}

    /** Lightweight execution guard between full sensing/selection updates. */
    default boolean canContinue() {
        return true;
    }
}
