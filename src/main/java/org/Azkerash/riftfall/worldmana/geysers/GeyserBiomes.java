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
        for(var climate:GeyserTransition.Climate.values())for(boolean deep:new boolean[]{false,true})for(double progress:new double[]{0,.25,.5,.7,.9})all.add(registry.getHolderOrThrow(GeyserTransition.biome(climate,deep,progress)));
        all.add(registry.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.RIVER));all.add(registry.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER));
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
                .filter(g->!g.legacy()&&(g.circular()?GeyserTransition.distance(g,cx*16.0+8,cz*16.0+8)<=12:g.contains(cx,cz))).orElse(null);
        }
    }
    /** Landscape-only buffer; it neither enlarges the mana pool nor the cave/deposit footprint. */
    public static GeyserRegion landscapeField(ServerLevel level,int cx,int cz){
        if(!level.dimension().equals(Level.OVERWORLD))return null;
        synchronized(level.getDataStorage()){
            var regions=GeyserRegions.get(level);if(regions.legacyAt(cx,cz))return null;
            return regions.candidate(Math.floorDiv(cx,GeyserLayout.CELL_CHUNKS),Math.floorDiv(cz,GeyserLayout.CELL_CHUNKS))
                .filter(g->!g.legacy()&&GeyserTransition.distance(g,cx*16+8,cz*16+8)<GeyserTransition.WIDTH+12).orElse(null);
        }
    }
    public static ChunkAccess assign(ServerLevel level,ChunkAccess chunk){
        if(!level.dimension().equals(Level.OVERWORLD))return chunk;
        prepare(level);
        var cp=chunk.getPos();var field=landscapeField(level,cp.x,cp.z);if(field==null)return chunk;
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        if(!field.circular()&&field.contains(cp.x,cp.z)){
            var biome=registry.getHolderOrThrow(key(field.zone(cp.x,cp.z)));
            chunk.fillBiomesFromNoise((x,y,z,sampler)->biome,level.getChunkSource().randomState().sampler());return chunk;
        }
        var surface=new java.util.ArrayList<net.minecraft.core.Holder<Biome>>(16);
        var inside=new boolean[16];
        for(int a=0;a<4;a++)for(int b=0;b<4;b++){
            int x=cp.getMinBlockX()+a*4+2,z=cp.getMinBlockZ()+b*4+2;
            var zone=GeyserShape.zone(field,x,z);
            if(zone!=GeyserZone.OUTSIDE){inside[a*4+b]=true;surface.add(registry.getHolderOrThrow(key(zone)));continue;}
            double distance=GeyserTransition.distance(field,x,z),t=GeyserTransition.progress(distance,x,z);
            if(distance>=GeyserTransition.WIDTH){surface.add(GeyserCoast.original(level,x,z));continue;}
            var environment=GeyserCoast.environment(level,field,x,z);var local=GeyserCoast.original(level,x,z);
            if(local.is(net.minecraft.tags.BiomeTags.IS_OCEAN)||GeyserTransition.ocean(environment.climate())&&!local.is(net.minecraft.tags.BiomeTags.IS_BEACH))environment=GeyserCoast.classify(local);
            var selected=GeyserTransition.biome(environment.climate(),environment.deep(),t);
            boolean channel=GeyserCoast.river(level,field,x,z)&&(t<.38||!GeyserTransition.ocean(environment.climate()));
            if(channel)selected=environment.climate()==GeyserTransition.Climate.COLD||environment.climate()==GeyserTransition.Climate.FROZEN_SEA?net.minecraft.world.level.biome.Biomes.FROZEN_RIVER:net.minecraft.world.level.biome.Biomes.RIVER;
            surface.add(!channel&&GeyserTransition.nativePatch(t,x,z)?local:registry.getHolderOrThrow(selected));
        }
        chunk.fillBiomesFromNoise((x,y,z,sampler)->y<8&&!inside[(x&3)*4+(z&3)]?level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(x,y,z,sampler):surface.get((x&3)*4+(z&3)),level.getChunkSource().randomState().sampler());
        return chunk;
    }
    private GeyserBiomes(){}
}
