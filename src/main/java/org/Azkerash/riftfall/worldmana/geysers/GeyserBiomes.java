package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Shared ring/transition resolver used by the world's actual biome source. */
public final class GeyserBiomes {
    public static java.util.Set<net.minecraft.core.Holder<Biome>> possible(ServerLevel level,java.util.Set<net.minecraft.core.Holder<Biome>> original){
        var all=new java.util.LinkedHashSet<net.minecraft.core.Holder<Biome>>(original);
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        for(var zone:new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER}){all.add(registry.getHolderOrThrow(key(zone)));all.add(registry.getHolderOrThrow(waterKey(zone,false)));all.add(registry.getHolderOrThrow(waterKey(zone,true)));}
        for(var climate:GeyserTransition.Climate.values())for(boolean deep:new boolean[]{false,true})for(double progress:new double[]{0,.25,.5,.7,.9})all.add(registry.getHolderOrThrow(GeyserTransition.biome(climate,deep,progress)));
        all.add(registry.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.RIVER));all.add(registry.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER));
        return java.util.Collections.unmodifiableSet(all);
    }
    public static ResourceKey<Biome> key(GeyserZone zone){
        String suffix=switch(zone){case CORE->"white_core";case INNER->"blue_inner";case MIDDLE->"gold_middle";case OUTER->"red_outer";case OUTSIDE->throw new IllegalArgumentException("Outside geyser");};
        return ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("riftfall","geyser_"+suffix));
    }
    /** Retained registry keys for already generated aquatic biome palettes; never newly assigned. */
    public static ResourceKey<Biome> waterKey(GeyserZone zone,boolean lake){
        String path=key(zone).location().getPath().substring("geyser_".length());
        return ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("riftfall","geyser_"+(lake?"lake_":"river_")+path));
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
    /** Quart coordinates match Minecraft's native biome source contract, including negative positions. */
    public static net.minecraft.core.Holder<Biome> resolve(ServerLevel level,net.minecraft.world.level.biome.BiomeSource original,int qx,int qy,int qz,net.minecraft.world.level.biome.Climate.Sampler sampler){
        int x=qx*4+2,z=qz*4+2;
        var field=landscapeField(level,Math.floorDiv(x,16),Math.floorDiv(z,16));
        if(field==null)return original.getNoiseBiome(qx,qy,qz,sampler);
        var zone=GeyserShape.zone(field,x,z);
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        if(zone!=GeyserZone.OUTSIDE)return registry.getHolderOrThrow(key(zone));
        // Preserve native cave biomes beneath the landscape-only transition belt.
        if(qy<8)return original.getNoiseBiome(qx,qy,qz,sampler);
        double distance=GeyserTransition.distance(field,x,z),t=GeyserTransition.progress(distance,x,z);
        var local=GeyserCoast.original(level,x,z);
        if(distance>=GeyserTransition.WIDTH)return local;
        var environment=GeyserCoast.environment(level,field,x,z);
        if(local.is(net.minecraft.tags.BiomeTags.IS_OCEAN)||GeyserTransition.ocean(environment.climate())&&!local.is(net.minecraft.tags.BiomeTags.IS_BEACH))environment=GeyserCoast.classify(local);
        return GeyserTransition.nativePatch(t,x,z)?local:registry.getHolderOrThrow(GeyserTransition.biome(environment.climate(),environment.deep(),t));
    }
    private GeyserBiomes(){}
}
