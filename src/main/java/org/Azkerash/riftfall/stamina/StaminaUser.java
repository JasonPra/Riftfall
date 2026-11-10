package org.Azkerash.riftfall.stamina;

/** Opt-in reusable stamina, regardless of species. Undead are exempt even when opted in. */
public interface StaminaUser {
    StaminaState staminaState();
    default boolean staminaConfident(){return false;}
    default boolean staminaFleeing(){return false;}
    void syncPanting(boolean panting);
}
