package org.Azkerash.riftfall.worldmana.geysers;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinCampLayoutTest {
    @Test void eligibilityIsSeededAndSelectsSomeButNotAllFields() {
        int eligible = 0, changed = 0;
        for (int i = 0; i < 1000; i++) {
            var field = new GeyserRegion(i * 512 + 160, -160, 129, 100);
            boolean chosen = GoblinCampLayout.eligible(123, field);
            assertEquals(chosen, GoblinCampLayout.eligible(123, field));
            if (chosen) eligible++;
            if (chosen != GoblinCampLayout.eligible(456, field)) changed++;
        }
        assertTrue(eligible > 270 && eligible < 400, "roughly one-third of fields");
        assertTrue(changed > 300);
    }
    @Test void sparseSitesStaySeparatedAcrossPositiveAndNegativeCellBoundaries() {
        int sites = 0;
        for (int cellX = -4; cellX < 4; cellX++) for (int cellZ = -4; cellZ < 4; cellZ++) {
            int count = 0;
            for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
                if (!GoblinCampLayout.candidate(123, cellX * 8 + x, cellZ * 8 + z)) continue;
                count++; sites++;
                assertTrue(x >= 2 && x <= 5 && z >= 2 && z <= 5);
            }
            assertTrue(count <= 1);
        }
        assertTrue(sites > 15 && sites < 50);
    }
    @Test void frontierHasCampFeatureButNoBiomeHasNaturalGoblins() throws Exception {
        for (String name : new String[]{"red_outer", "blue_inner", "gold_middle", "white_core"}) {
            try (var stream = getClass().getResourceAsStream("/data/riftfall/worldgen/biome/geyser_" + name + ".json")) {
                assertNotNull(stream);
                var biome = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                for (var category : biome.getAsJsonObject("spawners").entrySet())
                    for (var entry : category.getValue().getAsJsonArray())
                        assertFalse(entry.getAsJsonObject().get("type").getAsString().startsWith("riftfall:goblin"));
            }
        }
        try (var stream = getClass().getResourceAsStream("/data/riftfall/neoforge/biome_modifier/goblin_camps.json")) {
            var modifier = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("riftfall:geyser_red_outer", modifier.get("biomes").getAsString());
            assertEquals("surface_structures", modifier.get("step").getAsString());
        }
    }
}
