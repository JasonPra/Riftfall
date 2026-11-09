package org.Azkerash.riftfall.confidence;

/** Opt-in hook for event-driven confidence inputs. Implement on an entity using a per-instance controller. */
public interface ConfidenceParticipant {
    CombatConfidenceController confidenceController();
}
