package org.Azkerash.riftfall;

import org.Azkerash.riftfall.behavior.skills.MobSkillLoadout;
import yesman.epicfight.skill.SkillSlots;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobSkillLayoutTest {
    @Test void activeLayoutMatchesPlayersActiveCategoriesWithoutPassiveOrInternalSlots(){
        var slots=MobSkillLoadout.ACTIVE_SLOTS;
        assertEquals(5,slots.size());assertEquals(5,slots.stream().map(SkillSlots::category).distinct().count());
        assertTrue(slots.containsAll(java.util.List.of(SkillSlots.DODGE,SkillSlots.GUARD,SkillSlots.WEAPON_INNATE,SkillSlots.MOVER,SkillSlots.IDENTITY)));
        assertFalse(slots.contains(SkillSlots.COMBO_ATTACKS));assertFalse(slots.contains(SkillSlots.KNOCKDOWN_WAKEUP));assertFalse(slots.contains(SkillSlots.PASSIVE1));
    }
}
