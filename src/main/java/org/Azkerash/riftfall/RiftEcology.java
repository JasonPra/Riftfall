package org.Azkerash.riftfall;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.server.level.ServerLevel;
import org.Azkerash.riftfall.RiftData.Rift;

/** Capture entrance ecology once. Pocket-dimension biomes never choose its inhabitants. */
final class RiftEcology {
    static final Set<String> BOSS_ONLY = Set.of("irons_spellbooks:archevoker", "irons_spellbooks:grand_evoker", "irons_spellbooks:ice_spider");
    static String theme(String biome, boolean ocean, boolean cold, boolean desert, boolean swamp, boolean forest) {
        if (cold) return "frozen";
        if (ocean) return "aquatic";
        if (desert) return "arid";
        if (swamp) return "swamp";
        if (forest) return "woodland";
        if (biome.contains("nether") || biome.contains("basalt") || biome.contains("soul_sand")) return "nether";
        if (biome.contains("the_end") || biome.contains("end_")) return "end";
        return "temperate";
    }
    static void capture(ServerLevel world, Rift r) {
        var biome = world.getBiome(r.pos);
        r.sourceBiome = biome.unwrapKey().map(k -> k.location().toString()).orElse("minecraft:plains");
        r.theme = theme(r.sourceBiome, biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER),
            biome.value().coldEnoughToSnow(r.pos), r.sourceBiome.contains("desert") || biome.is(BiomeTags.IS_BADLANDS),
            r.sourceBiome.contains("swamp"), biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE));
        r.nativeMobs.clear();
        for (var entry : biome.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap())
            r.nativeMobs.add(BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).toString());
    }
    static boolean matches(String id, String theme, Set<String> natives) {
        if (BOSS_ONLY.contains(id)) return false;
        if (id.contains("summoned_") || id.contains("debug_") || id.contains("necromancer") || id.contains("catacombs")) return false;
        // Water-dependent species never go into the dry pocket arena.
        if (id.equals("minecraft:drowned")) return false;
        if (id.equals("minecraft:stray")) return theme.equals("frozen");
        if (id.equals("minecraft:husk")) return theme.equals("arid");
        if (id.equals("minecraft:slime") || id.equals("minecraft:witch")) return theme.equals("swamp");
        if (id.startsWith("riftfall:goblin")) return !Set.of("aquatic", "nether", "end").contains(theme);
        if (id.equals("minecraft:pillager") || id.equals("minecraft:vindicator") || id.equals("minecraft:evoker"))
            return Set.of("temperate", "woodland", "swamp").contains(theme);
        if (id.startsWith("irons_spellbooks:")) return switch (id) {
            case "irons_spellbooks:cryomancer" -> theme.equals("frozen");
            case "irons_spellbooks:pyromancer" -> theme.equals("arid") || theme.equals("nether");
            case "irons_spellbooks:apothecarist" -> theme.equals("swamp");
            case "irons_spellbooks:cultist", "irons_spellbooks:magehunter_vindicator" -> theme.equals("woodland");
            default -> false;
        };
        return natives.contains(id);
    }
    static boolean matches(EntityType<?> type, Rift r) {
        return matches(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), r.theme, r.nativeMobs);
    }
    static Set<String> defaults() { return new HashSet<>(Set.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper")); }
    private RiftEcology() {}
}
