package org.Azkerash.riftfall.client.titles;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local biome title preferences; no server synchronization. */
public final class ArrivalTitleConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Display a fading area name when entering a biome.").define("enabled", true);
        SPEC = b.build();
    }
    private ArrivalTitleConfig() {}
}
