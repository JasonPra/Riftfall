package org.Azkerash.riftfall.mana;
/** Available on every Mob; add-ons use MobMana instead of goblin-specific methods. */
public interface ManaUser {
    ManaState manaState();
    boolean hasManaState();
}
