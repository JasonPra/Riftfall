package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Samples the original generator, without terrain loads, for matching coasts and native river mouths. */
public final class GeyserCoast {
    public record Environment(GeyserTransition.Climate climate,boolean deep,Holder<Biome> original){}
    public record Outlet(int x,int z,int floor,boolean connected){}
    private record Key(long field,boolean north){}
    private static final Map<ServerLevel,Map<Key,Outlet>> OUTLETS=new WeakHashMap<>();
    public static Environment environment(ServerLevel level,GeyserRegion g,int x,int z){
        double dx=x-g.centerX(),dz=z-g.centerZ(),radius=Math.max(1,Math.max(Math.abs(dx),Math.abs(dz)));
        double probe=g.halfWidth()*16.0+8+GeyserTransition.WIDTH+64;
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
    public static Holder<Biome> original(ServerLevel level,int x,int z){return level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(x>>2,level.getSeaLevel()>>2,z>>2,level.getChunkSource().randomState().sampler());}
    static Outlet outlet(ServerLevel level,GeyserRegion g,boolean north){
        Map<Key,Outlet> cache;synchronized(OUTLETS){cache=OUTLETS.computeIfAbsent(level,l->new ConcurrentHashMap<>());}
        return cache.computeIfAbsent(new Key(GeyserLedger.key(g),north),k->{
            int z=(int)(g.centerZ()+(north?-1:1)*(g.halfWidth()*16+8+GeyserTransition.WIDTH));
            int center=(int)GeyserTerrain.riverCenter(g,z);Outlet ocean=null;
            for(int radius=0;radius<=512;radius+=8)for(int sign:radius==0?new int[]{1}:new int[]{-1,1}){
                int x=center+radius*sign;var b=original(level,x,z);
                if(b.is(BiomeTags.IS_RIVER)||ocean==null&&b.is(BiomeTags.IS_OCEAN)){
                    int floor=level.getChunkSource().getGenerator().getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,level,level.getChunkSource().randomState())-1;
                    if(floor>=level.getSeaLevel()-1)continue;
                    var candidate=new Outlet(x,z,floor,true);if(b.is(BiomeTags.IS_RIVER))return candidate;else ocean=candidate;
                }
            }
            return ocean!=null?ocean:new Outlet(center,z,level.getSeaLevel()+2,false);
        });
    }
    public static double channelCenter(ServerLevel level,GeyserRegion g,int z){
        double signed=Math.abs(z-g.centerZ())-(g.halfWidth()*16+8);if(signed<=0)return GeyserTerrain.riverCenter(g,z);
        boolean north=z<g.centerZ();var outlet=outlet(level,g,north);
        double t=Math.clamp(signed/GeyserTransition.WIDTH,0,1),s=t*t*(3-2*t);
        double startZ=g.centerZ()+(north?-1:1)*(g.halfWidth()*16+8),start=GeyserTerrain.riverCenter(g,startZ);
        return start+(outlet.x-start)*s+12*Math.sin(t*Math.PI*2)*Math.sin(t*Math.PI);
    }
    public static int channelBed(ServerLevel level,GeyserRegion g,int x,int z,int original){
        double distance=Math.abs(z-g.centerZ())-(g.halfWidth()*16+8);
        if(distance<0||distance>=GeyserTransition.WIDTH)return Integer.MAX_VALUE;
        var outlet=outlet(level,g,z<g.centerZ());if(!outlet.connected)return Integer.MAX_VALUE;
        double t=distance/GeyserTransition.WIDTH,s=t*t*(3-2*t),width=GeyserTerrain.riverWidth(g,z)*(1-s)+8*s;
        double offset=Math.abs(x-channelCenter(level,g,z))/width;if(offset>1.5)return Integer.MAX_VALUE;
        double bed=(level.getSeaLevel()-6)*(1-s)+outlet.floor*s;
        double banks=Math.clamp((offset-.7)/.8,0,1);banks=banks*banks*(3-2*banks);
        return (int)Math.round(bed+(original-bed)*banks);
    }
    public static boolean river(ServerLevel level,GeyserRegion g,int x,int z){return Math.abs(z-g.centerZ())>=g.halfWidth()*16&&outlet(level,g,z<g.centerZ()).connected&&Math.abs(x-channelCenter(level,g,z))<12;}
    private GeyserCoast(){}
}
