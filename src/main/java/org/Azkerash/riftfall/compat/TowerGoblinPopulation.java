package org.Azkerash.riftfall.compat;

import dev.kubas.playersystem.dungeon.DungeonRank;
import dev.kubas.playersystem.dungeon.motif.MobRole;
import dev.kubas.playersystem.dungeon.motif.MobTable;
import dev.kubas.playersystem.dungeon.motif.SpawnEntry;

import java.util.Optional;

/** Reserves an existing population pack, within the tower's normal mob budget. */
public final class TowerGoblinPopulation {
    public static Optional<SpawnEntry> openingPack(MobTable table, DungeonRank rank) {
        return table.entries().stream()
                .filter(
                        entry ->
                                entry.entity().toString().equals("player_system:goblin_scavenger")
                                        && entry.role() == MobRole.WARRIOR
                                        && entry.weight() > 0
                                        && entry.covers(rank))
                .findFirst()
                .map(
                        entry ->
                                new SpawnEntry(
                                        entry.entity(),
                                        entry.weight(),
                                        entry.role(),
                                        entry.optional(),
                                        entry.min(),
                                        entry.max(),
                                        3,
                                        3,
                                        entry.affixes(),
                                        0));
    }

    private TowerGoblinPopulation() {}
}
