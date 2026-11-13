package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.*;

/** Samples the original generator, without terrain loads, for the existing outer biome transition. */
public final class GeyserCoast {
    public record Environment(GeyserTransition.Climate climate,boolean deep,Holder<Biome> original){}
    public static Environment environment(ServerLevel level,GeyserRegion g,int x,int z){
        double dx=x-g.centerX(),dz=z-g.centerZ(),radius=Math.max(1,g.circular()?Math.hypot(dx,dz):Math.max(Math.abs(dx),Math.abs(dz)));
        double probe=GeyserShape.radius(g)+GeyserShape.EDGE_NOISE+GeyserTransition.WIDTH+64;
        int px=(int)(g.centerX()+dx/radius*probe),pz=(int)(g.centerZ()+dz/radius*probe);
        return classify(original(level,px,pz));
    }
    public static Environment classify(Holder<Biome> biome){
        var c=GeyserTransition.Climate.TEMPERATE;
        float temperature=biome.value().getBaseTemperature();
        if(biome.is(BiomeTags.IS_OCEAN))c=biome.is(Biomes.FROZEN_OCEAN)||biome.is(Biomes.DEEP_FROZEN_OCEAN)?GeyserTransition.Climate.FROZEN_SEA:biome.is(Biomes.COLD_OCEAN)||biome.is(Biomes.DEEP_COLD_OCEAN)?GeyserTransition.Climate.COLD_SEA:biome.is(Biomes.WARM_OCEAN)||biome.is(Biomes.LUKEWARM_OCEAN)||biome.is(Biomes.DEEP_LUKEWARM_OCEAN)?GeyserTransition.Climate.WARM_SEA:GeyserTransition.Climate.SEA;
        else if(biome.is(BiomeTags.IS_JUNGLE))c=GeyserTransition.Climate.WARM;
        else if(temperature<.3)c=GeyserTransition.Climate.COLD;
        else if(temperature>1.1)c=GeyserTransition.Climate.DRY;
        else if(biome.is(BiomeTags.IS_MOUNTAIN))c=GeyserTransition.Climate.MOUNTAIN;
        return new Environment(c,biome.is(BiomeTags.IS_DEEP_OCEAN),biome);
    }
    public static Holder<Biome> original(ServerLevel level,int x,int z){return GeyserBiomeSource.original(level.getChunkSource().getGenerator().getBiomeSource()).getNoiseBiome(x>>2,level.getSeaLevel()>>2,z>>2,level.getChunkSource().randomState().sampler());}
    private GeyserCoast(){}
}
