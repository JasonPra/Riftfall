package org.Azkerash.riftfall;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue SPAWN_SECONDS = B.comment("Seconds between random rift spawn attempts while overworld players are online.").defineInRange("spawnSeconds", 300, 10, 86400);
    public static final ModConfigSpec.IntValue BREAK_SECONDS = B.comment("Loaded server time before an uncleared rift breaks, including while players are inside.").defineInRange("breakSeconds", 600, 10, 86400);
    public static final ModConfigSpec.IntValue WAVE_SECONDS = B.comment("Seconds between dungeon break waves when the rift chunk is loaded.").defineInRange("waveSeconds", 15, 1, 3600);
    public static final ModConfigSpec.IntValue MAX_RIFTS = B.defineInRange("maxRifts", 5, 1, 100);
    public static final ModConfigSpec.IntValue ENEMIES = B.defineInRange("arenaEnemies", 6, 1, 100);
    public static final ModConfigSpec.IntValue MAX_OUTSIDE = B.comment("Maximum living tracked break mobs per rift. Spawning resumes after kills.").defineInRange("maxOutsideMobs", 20, 1, 200);
    static final ModConfigSpec SPEC = B.build();
    private Config() {}
}
