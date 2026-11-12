package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in development probe. Never include in the release source set. */
@EventBusSubscriber(modid="riftfall")
public final class WorldgenBaselineProbe {
    @SubscribeEvent public static void run(ServerStartedEvent event) {
        var level=event.getServer().overworld();
        var generator=level.getChunkSource().getGenerator();
        var random=level.getChunkSource().randomState();
        System.out.println("BASELINE_SEED="+level.getSeed());
        boolean river=false,coast=false,inland=false;
        for(int z=-4096;z<=4096;z+=64) for(int x=-4096;x<=4096;x+=64){
            var biome=GeyserCoast.original(level,x,z);
            String kind=null;
            if(!river&&biome.is(Biomes.RIVER)){river=true;kind="river";}
            else if(!coast&&biome.is(BiomeTags.IS_BEACH)){coast=true;kind="coast";}
            else if(!inland&&biome.is(Biomes.PLAINS)){inland=true;kind="inland";}
            if(kind!=null){
                for(int offset:new int[]{-1,0,1,15,16}){
                    int px=x+offset;
                    int surface=generator.getBaseHeight(px,z,Heightmap.Types.WORLD_SURFACE_WG,level,random);
                    int floor=generator.getBaseHeight(px,z,Heightmap.Types.OCEAN_FLOOR_WG,level,random);
                    System.out.println("BASELINE_SAMPLE "+kind+" x="+px+" z="+z+" biome="+GeyserCoast.original(level,px,z).unwrapKey().orElseThrow().location()+" surface="+surface+" oceanFloor="+floor);
                }
            }
            if(river&&coast&&inland){event.getServer().halt(false);return;}
        }
        throw new IllegalStateException("Baseline search did not find every environment");
    }
}
