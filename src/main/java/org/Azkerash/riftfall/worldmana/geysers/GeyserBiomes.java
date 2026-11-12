package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Real saved biome palettes, assigned before terrain surfacing and biome decoration. */
public final class GeyserBiomes {
    private static final java.util.Map<net.minecraft.world.level.biome.BiomeSource,java.util.Set<net.minecraft.core.Holder<Biome>>> SOURCES=new java.util.WeakHashMap<>();
    public static synchronized java.util.Set<net.minecraft.core.Holder<Biome>> possible(net.minecraft.world.level.biome.BiomeSource source,java.util.Set<net.minecraft.core.Holder<Biome>> original){return SOURCES.getOrDefault(source,original);}
    private static synchronized void prepare(ServerLevel level){
        var generator=level.getChunkSource().getGenerator();var source=generator.getBiomeSource();
        if(SOURCES.containsKey(source))return;
        var all=new java.util.LinkedHashSet<net.minecraft.core.Holder<Biome>>();
        source.possibleBiomes().stream().sorted(java.util.Comparator.comparing(h->h.unwrapKey().orElseThrow().location().toString())).forEach(all::add);var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        for(var zone:new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER})all.add(registry.getHolderOrThrow(key(zone)));
        SOURCES.put(source,java.util.Collections.unmodifiableSet(all));generator.refreshFeaturesPerStep();
    }
    public static ResourceKey<Biome> key(GeyserZone zone){
        String suffix=switch(zone){case CORE->"white_core";case INNER->"blue_inner";case MIDDLE->"gold_middle";case OUTER->"red_outer";case OUTSIDE->throw new IllegalArgumentException("Outside geyser");};
        return ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("riftfall","geyser_"+suffix));
    }
    public static GeyserRegion field(ServerLevel level,int cx,int cz){
        if(!level.dimension().equals(Level.OVERWORLD))return null;
        synchronized(level.getDataStorage()){
            return GeyserRegions.get(level).candidate(Math.floorDiv(cx,GeyserLayout.CELL_CHUNKS),Math.floorDiv(cz,GeyserLayout.CELL_CHUNKS))
                .filter(g->!g.legacy()&&g.contains(cx,cz)).orElse(null);
        }
    }
    public static ChunkAccess assign(ServerLevel level,ChunkAccess chunk){
        if(!level.dimension().equals(Level.OVERWORLD))return chunk;
        prepare(level);
        var cp=chunk.getPos();var field=field(level,cp.x,cp.z);if(field==null)return chunk;
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key(field.zone(cp.x,cp.z)));
        chunk.fillBiomesFromNoise((x,y,z,sampler)->biome,level.getChunkSource().randomState().sampler());
        return chunk;
    }
    private GeyserBiomes(){}
}
