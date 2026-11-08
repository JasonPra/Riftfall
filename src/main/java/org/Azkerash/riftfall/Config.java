package org.Azkerash.riftfall;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue ESSENCE_PER_SECOND = B.comment("Soul essence gained per second in each loaded overworld chunk. Unloaded chunks pause.").defineInRange("essencePerSecond", 1.0, 0.0, 1_000_000_000.0);
    public static final ModConfigSpec.DoubleValue CRITICAL_MASS = B.comment("Essence required to roll for a rift; passive accumulation caps here. The world spawn chunk starts critical once per world.").defineInRange("criticalMass", 600.0, 1.0, 1_000_000_000.0);
    public static final ModConfigSpec.IntValue ESSENCE_ROLL_SECONDS = B.comment("Seconds between rift rolls in critical chunks.").defineInRange("essenceRollSeconds", 10, 1, 86400);
    public static final ModConfigSpec.DoubleValue ESSENCE_SPAWN_CHANCE = B.comment("Spawn probability per critical chunk per roll (0 disables natural rifts, 1 guarantees a roll succeeds if a site is available).").defineInRange("essenceSpawnChance", 0.1, 0.0, 1.0);
    public static final ModConfigSpec.IntValue BREAK_SECONDS = B.comment("Loaded server time before an uncleared rift breaks, including while players are inside.").defineInRange("breakSeconds", 600, 10, 86400);
    public static final ModConfigSpec.IntValue WAVE_SECONDS = B.comment("Seconds between dungeon break waves when the rift chunk is loaded.").defineInRange("waveSeconds", 15, 1, 3600);
    public static final ModConfigSpec.IntValue MAX_RIFTS = B.defineInRange("maxRifts", 5, 1, 100);
    public static final ModConfigSpec.IntValue ENEMIES = B.defineInRange("arenaEnemies", 6, 1, 100);
    public static final ModConfigSpec.IntValue MAX_OUTSIDE = B.comment("Maximum living tracked break mobs per rift. Spawning resumes after kills.").defineInRange("maxOutsideMobs", 20, 1, 200);
    static final ModConfigSpec SPEC = B.build();
    private Config() {}
}
