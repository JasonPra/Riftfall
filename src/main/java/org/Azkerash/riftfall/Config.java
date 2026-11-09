package org.Azkerash.riftfall;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue BASE_ESSENCE_PER_SECOND = B.comment("Soul essence gained per second in each loaded overworld chunk. Unloaded chunks pause.").defineInRange("baseEssencePerSecond", 0.05, 0.0, 1_000_000_000.0);
    public static final ModConfigSpec.DoubleValue MATURE_CRYSTAL_BONUS = B.comment("Extra essence per second per fully mature cluster in a loaded chunk.").defineInRange("matureCrystalEssenceBonus", 0.05, 0.0, 1_000_000_000.0);
    public static final ModConfigSpec.DoubleValue RIFT_AMBIENT_INTAKE = B.comment("Essence per second a loaded rift absorbs from its root chunk into its construction bank and permanent power.").defineInRange("riftAmbientIntake", 0.1, 0.0, 1000000.0);
    public static final ModConfigSpec.DoubleValue RIFT_POOL_DRAW = B.comment("Maximum essence drawn per second from EACH connected loaded donor chunk while a root refills (previously shared by the entire pool).").defineInRange("riftPoolDrawPerSecond", 10.0, 0.0, 1000000.0);
    public static final ModConfigSpec.DoubleValue RIFT_POOL_TOTAL_DRAW = B.comment("Maximum combined donor essence transferred per second to each active or dormant root.").defineInRange("riftPoolTotalDrawPerSecond", 100.0, 0.0, 1000000.0);
    public static final ModConfigSpec.IntValue RIFT_DORMANT_SECONDS = B.defineInRange("riftDormantSeconds", 60, 1, 86400);
    public static final ModConfigSpec.IntValue RIFT_DEATH_ESSENCE = B.defineInRange("riftDeathEssence", 50, 1, 1000000);
    public static final ModConfigSpec.IntValue RIFT_UPGRADE_ESSENCE = B.defineInRange("riftUpgradeEssence", 50, 1, 1000000);
    public static final ModConfigSpec.IntValue RIFT_MAX_LEVEL = B.defineInRange("riftMaxLevel", 10, 1, 10);
    public static final ModConfigSpec.DoubleValue CRITICAL_MASS = B.comment("Essence required to roll for a rift; passive accumulation caps here.").defineInRange("criticalMass", 600.0, 1.0, 1_000_000_000.0);
    public static final ModConfigSpec.IntValue ESSENCE_ROLL_SECONDS = B.comment("Seconds between rift rolls in critical chunks.").defineInRange("essenceRollSeconds", 10, 1, 86400);
    public static final ModConfigSpec.DoubleValue ESSENCE_SPAWN_CHANCE = B.comment("Spawn probability per critical chunk per roll (0 disables natural rifts, 1 guarantees a roll succeeds if a site is available).").defineInRange("essenceSpawnChance", 0.1, 0.0, 1.0);
    public static final ModConfigSpec.IntValue BREAK_SECONDS = B.comment("Loaded server time before an uncleared rift breaks, including while players are inside.").defineInRange("breakSeconds", 600, 10, 86400);
    public static final ModConfigSpec.IntValue WAVE_SECONDS = B.comment("Seconds between dungeon break waves when the rift chunk is loaded.").defineInRange("waveSeconds", 15, 1, 3600);
    public static final ModConfigSpec.IntValue MAX_RIFTS = B.defineInRange("maxRifts", 5, 1, 100);
    public static final ModConfigSpec.IntValue ENEMIES = B.defineInRange("arenaEnemies", 6, 1, 100);
    public static final ModConfigSpec.IntValue MAX_OUTSIDE = B.comment("Maximum living tracked break mobs per rift. Spawning resumes after kills.").defineInRange("maxOutsideMobs", 20, 1, 200);
    public static final ModConfigSpec.IntValue CRYSTAL_GROWTH_SECONDS = B.comment("Seconds between crystal growth or spread attempts per cluster.").defineInRange("crystalGrowthSeconds", 20, 1, 86400);
    public static final ModConfigSpec.DoubleValue CRYSTAL_GROWTH_COST = B.defineInRange("crystalGrowthCost", 10.0, 0.1, 1_000_000_000.0);
    public static final ModConfigSpec.DoubleValue CRYSTAL_SEED_COST = B.defineInRange("crystalSeedCost", 20.0, 0.1, 1_000_000_000.0);
    public static final ModConfigSpec.IntValue CRYSTAL_CAVE_SECONDS = B.defineInRange("crystalCaveSeedSeconds", 60, 1, 86400);
    public static final ModConfigSpec.DoubleValue CRYSTAL_CAVE_CHANCE = B.comment("Chance per high-essence loaded chunk per cave seeding check.").defineInRange("crystalCaveSeedChance", 0.001, 0.0, 1.0);
    public static final ModConfigSpec.IntValue CRYSTAL_CAVE_RARITY = B.comment("Additional 1-in-N rarity gate for runtime cave seeds. Applies in existing worlds too; default makes the existing cave seed chance 100 times rarer.").defineInRange("crystalCaveSeedRarity", 100, 1, 1000000);
    static final ModConfigSpec SPEC = B.build();
    private Config() {}
}
