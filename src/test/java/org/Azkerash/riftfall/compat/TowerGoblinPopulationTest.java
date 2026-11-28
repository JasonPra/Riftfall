package org.Azkerash.riftfall.compat;

import static org.junit.jupiter.api.Assertions.*;

import dev.kubas.playersystem.dungeon.DungeonRank;
import dev.kubas.playersystem.dungeon.motif.*;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

class TowerGoblinPopulationTest {
    private SpawnEntry entry(String species, DungeonRank min, DungeonRank max) {
        return new SpawnEntry(
                ResourceLocation.parse("player_system:" + species),
                8,
                MobRole.WARRIOR,
                false,
                min,
                max,
                2,
                4);
    }

    @Test
    void eligibleFloorReservesThreeTrueGoblinsWithoutChangingItsTable() {
        var goblins = entry("goblin_scavenger", DungeonRank.E, DungeonRank.E);
        var hobgoblins = entry("hobgoblin_guard", DungeonRank.D, DungeonRank.B);
        var table = new MobTable(List.of(goblins, hobgoblins));
        var pack = TowerGoblinPopulation.openingPack(table, DungeonRank.E).orElseThrow();
        assertEquals(goblins.entity(), pack.entity());
        assertEquals(3, pack.minCount());
        assertEquals(3, pack.maxCount());
        assertEquals(List.of(goblins, hobgoblins), table.entries());
        assertTrue(TowerGoblinPopulation.openingPack(table, DungeonRank.D).isEmpty());
    }

    @Test
    void hobgoblinOnlyFloorDoesNotReceiveAGoblinReplacementPack() {
        var table = new MobTable(List.of(entry("hobgoblin_guard", DungeonRank.D, DungeonRank.B)));
        assertTrue(TowerGoblinPopulation.openingPack(table, DungeonRank.D).isEmpty());
    }
}
