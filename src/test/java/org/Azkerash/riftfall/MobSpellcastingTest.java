package org.Azkerash.riftfall;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobSpellcastingTest {
    @Test void onlyApprovedLivingHumanoidsCanCast() {
        assertTrue(MobSpellcasting.canLearn("minecraft:pillager", false));
        assertTrue(MobSpellcasting.canLearn("riftfall:goblin_shaman", false));
        assertFalse(MobSpellcasting.canLearn("riftfall:goblin", false));
        assertFalse(MobSpellcasting.canLearn("riftfall:goblin_archer", false));
        for (String id : Set.of("minecraft:zombie", "minecraft:skeleton", "minecraft:zombified_piglin", "minecraft:spider", "minecraft:cow"))
            assertFalse(MobSpellcasting.canLearn(id, id.contains("zombie") || id.contains("skeleton")));
        assertFalse(MobSpellcasting.canLearn("minecraft:pillager", true));
    }
}
