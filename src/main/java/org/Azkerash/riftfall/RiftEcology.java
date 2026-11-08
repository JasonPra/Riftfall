package org.Azkerash.riftfall;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.server.level.ServerLevel;
import org.Azkerash.riftfall.RiftData.Rift;

/** Entrance-specific, saved weighted rosters. Iron's wizards remain overworld encounters. */
final class RiftEcology {
    static final Set<String> BOSS_ONLY = Set.of("irons_spellbooks:archevoker", "irons_spellbooks:grand_evoker", "irons_spellbooks:ice_spider");
    static final Map<String,Integer> PLAINS = Map.of("riftfall:goblin",7,"riftfall:goblin_archer",3,
        "riftfall:goblin_shaman",2,"minecraft:zombie",2,"minecraft:skeleton",2);
    static String theme(String biome, boolean ocean, boolean cold, boolean desert, boolean swamp, boolean forest) {
        return cold ? "frozen" : biome.equals("minecraft:plains") || biome.equals("minecraft:sunflower_plains") ? "plains" : "vanilla";
    }
    static boolean overworldOnly(String id) { return id.startsWith("irons_spellbooks:") && !id.equals("irons_spellbooks:ice_spider"); }
    static void capture(ServerLevel world, Rift r) {
        var biome=world.hasChunkAt(r.pos) ? world.getBiome(r.pos) :
            world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                    net.minecraft.resources.ResourceLocation.tryParse(r.sourceBiome)==null ? net.minecraft.resources.ResourceLocation.withDefaultNamespace("plains") : net.minecraft.resources.ResourceLocation.parse(r.sourceBiome)))
                .orElseGet(()->world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS));
        r.sourceBiome=biome.unwrapKey().map(k->k.location().toString()).orElse("minecraft:plains");
        r.theme=theme(r.sourceBiome,biome.is(BiomeTags.IS_OCEAN),biome.value().coldEnoughToSnow(r.pos),false,false,false);
        r.nativeMobs.clear(); r.roster.clear();
        for (var entry : biome.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap()) {
            String id=BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).toString();
            r.nativeMobs.add(id);
            if (id.startsWith("minecraft:") && RiftMobs.eligible(entry.type) && !(r.theme.equals("frozen") && id.equals("minecraft:drowned")))
                r.roster.put(id,entry.getWeight().asInt());
        }
        if (r.theme.equals("plains")) { r.roster.clear(); r.roster.putAll(PLAINS); }
        r.rosterVersion=2;
    }
    static boolean matches(String id,String theme,Set<String> natives) {
        if (!RiftMobs.allowedId(id) || BOSS_ONLY.contains(id) || overworldOnly(id)) return false;
        if (theme.equals("plains")) return PLAINS.containsKey(id);
        if (theme.equals("frozen") && (id.equals("minecraft:drowned") || id.equals("minecraft:husk"))) return false;
        return id.startsWith("minecraft:") && natives.contains(id);
    }
    static boolean matches(EntityType<?> type,Rift r) { return weight(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),r)>0; }
    static int weight(String id,Rift r) {
        if (!matches(id,r.theme,r.nativeMobs)) return 0;
        return r.roster.getOrDefault(id,r.theme.equals("plains") ? PLAINS.getOrDefault(id,0) : 10);
    }
    static Set<String> defaults() { return new HashSet<>(Set.of("minecraft:zombie","minecraft:skeleton","minecraft:spider","minecraft:creeper")); }
    private RiftEcology() {}
}
