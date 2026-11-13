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
    public record Outlet(int x,int z,int floor,boolean connected,int edgeZ){}
    private record Key(long field,boolean north){}
    private static final Map<ServerLevel,Map<Key,Outlet>> OUTLETS=new WeakHashMap<>();
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
    public static Holder<Biome> original(ServerLevel level,int x,int z){return level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(x>>2,level.getSeaLevel()>>2,z>>2,level.getChunkSource().randomState().sampler());}
    static Outlet outlet(ServerLevel level,GeyserRegion g,boolean north){
        Map<Key,Outlet> cache;synchronized(OUTLETS){cache=OUTLETS.computeIfAbsent(level,l->new ConcurrentHashMap<>());}
        return cache.computeIfAbsent(new Key(GeyserLedger.key(g),north),k->{
            int edge=riverEdge(g,north);
            int z=edge+(north?-1:1)*GeyserTransition.WIDTH;
            int center=(int)GeyserTerrain.riverCenter(g,z);Outlet ocean=null;
            for(int radius=0;radius<=512;radius+=8)for(int sign:radius==0?new int[]{1}:new int[]{-1,1}){
                int x=center+radius*sign;var b=original(level,x,z);
                if(b.is(BiomeTags.IS_RIVER)||ocean==null&&b.is(BiomeTags.IS_OCEAN)){
                    int floor=level.getChunkSource().getGenerator().getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,level,level.getChunkSource().randomState())-1;
                    if(floor>=level.getSeaLevel()-1)continue;
                    var candidate=new Outlet(x,z,floor,true,edge);if(b.is(BiomeTags.IS_RIVER))return candidate;else ocean=candidate;
                }
            }
            return ocean!=null?ocean:new Outlet(center,z,level.getSeaLevel()+2,false,edge);
        });
    }
    private static int riverEdge(GeyserRegion g,boolean north){
        if(!g.circular())return (int)(g.centerZ()+(north?-1:1)*(g.halfWidth()*16+8));
        double low=GeyserShape.radius(g)*.5,high=GeyserShape.radius(g)+GeyserShape.EDGE_NOISE+16;
        for(int i=0;i<24;i++){
            double mid=(low+high)/2,z=g.centerZ()+(north?-1:1)*mid;
            if(GeyserTransition.distance(g,GeyserTerrain.riverCenter(g,z),z)<0)low=mid;else high=mid;
        }
        return (int)Math.round(g.centerZ()+(north?-1:1)*(low+high)/2);
    }
    public static double channelCenter(ServerLevel level,GeyserRegion g,int z){
        if(Math.abs(z-g.centerZ())<GeyserShape.radius(g)*.5)return GeyserTerrain.riverCenter(g,z);
        boolean north=z<g.centerZ();var outlet=outlet(level,g,north);
        double signed=(north?-1:1)*(z-outlet.edgeZ);if(signed<=0)return GeyserTerrain.riverCenter(g,z);
        double t=Math.clamp(signed/GeyserTransition.WIDTH,0,1),s=t*t*(3-2*t);
        double start=GeyserTerrain.riverCenter(g,outlet.edgeZ);
        return start+(outlet.x-start)*s+12*Math.sin(t*Math.PI*2)*Math.sin(t*Math.PI);
    }
    private record StageKey(long field,boolean north){}
    private static final Map<ServerLevel,Map<StageKey,int[]>> STAGES=new WeakHashMap<>();
    private static int channelLevel(ServerLevel level,GeyserRegion g,int z,Outlet outlet,double t){
        Map<StageKey,int[]> cache;
        synchronized(STAGES){cache=STAGES.computeIfAbsent(level,l->new ConcurrentHashMap<>());}
        if(cache.size()>2048)cache.clear();
        boolean north=z<g.centerZ();
        var stages=cache.computeIfAbsent(new StageKey(GeyserLedger.key(g),north),key->{
            int count=GeyserTransition.WIDTH/16,sea=level.getSeaLevel();var values=new int[count+1];
            values[0]=Math.max(sea-1,GeyserTerrain.riverLevel(g,outlet.edgeZ));
            for(int i=1;i<=count;i++){
                int zz=outlet.edgeZ+(north?-1:1)*i*16;
                int x=(int)Math.round(channelCenter(level,g,zz));
                int nativeFloor=level.getChunkSource().getGenerator().getBaseHeight(x,zz,Heightmap.Types.OCEAN_FLOOR_WG,level,level.getChunkSource().randomState())-1;
                double d=GeyserTransition.distance(g,x,zz),blend=GeyserTransition.progress(d,x,zz);
                int ground=(int)Math.floor(GeyserTerrain.ground(g,x,zz));
                int inland=GeyserTransition.floor(GeyserTransition.Climate.TEMPERATE,blend,ground,nativeFloor,sea);
                int coast=GeyserTransition.floor(GeyserTransition.Climate.SEA,blend,ground,nativeFloor,sea);
                double ocean=Math.clamp((sea+10-nativeFloor)/24.0,0,1);ocean=ocean*ocean*(3-2*ocean);
                int reference=(int)Math.round(inland+(coast-inland)*ocean);
                double progress=i/(double)count,smooth=progress*progress*(3-2*progress);
                int grade=(int)Math.floor(values[0]*(1-smooth)+(sea-1)*smooth);
                // Once the route descends, later native ridges cannot make it flow uphill again.
                values[i]=Math.min(values[i-1],Math.max(sea-1,Math.min(grade,reference-1)));
            }
            return values;
        });
        double position=Math.clamp(t,0,1)*(stages.length-1);int index=Math.min(stages.length-2,(int)position);
        return (int)Math.floor(stages[index]+(stages[index+1]-stages[index])*(position-index));
    }
    /** A graded handoff; closed routes remain inside their terminal lakes. */
    public static GeyserTerrain.Column channel(ServerLevel level,GeyserRegion g,int x,int z,int original){
        var dry=new GeyserTerrain.Column(original,Integer.MIN_VALUE,GeyserTerrain.WaterKind.NONE);
        boolean north=z<g.centerZ();
        if(!GeyserTerrain.openEnd(g,north)||Math.abs(z-g.centerZ())<GeyserShape.radius(g)*.5)return dry;
        var outlet=outlet(level,g,north);
        double distance=(north?-1:1)*(z-outlet.edgeZ);
        if(distance<0||distance>=GeyserTransition.WIDTH||!outlet.connected)return dry;
        double t=distance/GeyserTransition.WIDTH,s=t*t*(3-2*t);
        double width=GeyserTerrain.riverWidth(g,z)*(1-s)+6*s;
        double offset=Math.abs(x-channelCenter(level,g,z))/width;if(offset>4)return dry;
        int water=channelLevel(level,g,z,outlet,t);
        double bed=(water-2)*(1-s)+outlet.floor*s;
        double banks=Math.clamp((offset-1)/3,0,1);banks=banks*banks*(3-2*banks);
        int floor=(int)Math.round(bed+(original-bed)*banks);
        return new GeyserTerrain.Column(Math.min(original,floor),water,floor<water?GeyserTerrain.WaterKind.RIVER:GeyserTerrain.WaterKind.NONE);
    }
    public static boolean river(ServerLevel level,GeyserRegion g,int x,int z){
        if(!GeyserTerrain.openEnd(g,z<g.centerZ())||Math.abs(z-g.centerZ())<GeyserShape.radius(g)*.5)return false;
        var outlet=outlet(level,g,z<g.centerZ());
        return (z<g.centerZ()?-1:1)*(z-outlet.edgeZ)>=0&&outlet.connected&&Math.abs(x-channelCenter(level,g,z))<12;
    }
    private GeyserCoast(){}
}
