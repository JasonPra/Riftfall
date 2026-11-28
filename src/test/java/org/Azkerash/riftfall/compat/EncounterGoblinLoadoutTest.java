package org.Azkerash.riftfall.compat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EncounterGoblinLoadoutTest {
    @Test
    void smallGroupsGetSupportAndLargerGroupsMixArchersWithWarriors() {
        assertEquals(EncounterGoblinLoadout.Role.SHAMAN, EncounterGoblinLoadout.role(0, 0, 0, 3));
        assertEquals(EncounterGoblinLoadout.Role.WARRIOR, EncounterGoblinLoadout.role(1, 0, 0, 0));
        assertEquals(EncounterGoblinLoadout.Role.ARCHER, EncounterGoblinLoadout.role(1, 1, 0, 3));
        int archers = 0, warriors = 0;
        for (int roll = 0; roll < 4; roll++) {
            var role = EncounterGoblinLoadout.role(1, 2, 1, roll);
            if (role == EncounterGoblinLoadout.Role.ARCHER) {
                archers++;
            } else if (role == EncounterGoblinLoadout.Role.WARRIOR) {
                warriors++;
            }
        }
        assertEquals(2, archers);
        assertEquals(2, warriors);
        assertEquals(EncounterGoblinLoadout.Role.SHAMAN, EncounterGoblinLoadout.role(0, 4, 2, 3));
    }

    @Test
    void floorPopulationHasAtMostTwoShamansAndFavorsCombatRoles() {
        int shamans = 0, warriors = 0, archers = 0;
        for (int slot = 0; slot < 60; slot++) {
            switch (EncounterGoblinLoadout.role(shamans, warriors, archers, slot % 4)) {
                case SHAMAN -> shamans++;
                case WARRIOR -> warriors++;
                case ARCHER -> archers++;
            }
            assertEquals(slot < 13 ? 1 : 2, shamans);
        }
        assertEquals(2, shamans);
        assertTrue(warriors >= 25);
        assertTrue(archers >= 25);
    }
}
