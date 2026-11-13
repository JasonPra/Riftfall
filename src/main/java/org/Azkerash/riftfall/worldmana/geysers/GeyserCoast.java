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
    public record Outlet(int x,int z,int floor,boolean connected,double edgeX,double edgeZ,double nx,double nz){}
    public record ChannelPoint(double x,double z){}
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
        var route=GeyserDrainage.of(g);var mouth=route.mouth();
        double dx=mouth.x()-g.centerX(),dz=mouth.z()-g.centerZ(),norm=Math.max(1,Math.hypot(dx,dz)),nx=dx/norm,nz=dz/norm;
        if(!route.external())return new Outlet((int)mouth.x(),(int)mouth.z(),mouth.water()-2,false,mouth.x(),mouth.z(),nx,nz);
        Map<Key,Outlet> cache;synchronized(OUTLETS){cache=OUTLETS.computeIfAbsent(level,l->new ConcurrentHashMap<>());}
        if(cache.size()>256)cache.clear();
        return cache.computeIfAbsent(new Key(GeyserLedger.key(g),route.north()),k->{
            double endX=mouth.x()+nx*GeyserTransition.WIDTH,endZ=mouth.z()+nz*GeyserTransition.WIDTH;Outlet ocean=null;
            for(int radius=0;radius<=512;radius+=8)for(int sign:radius==0?new int[]{1}:new int[]{-1,1}){
                int x=(int)Math.round(endX-nz*radius*sign),z=(int)Math.round(endZ+nx*radius*sign);var b=original(level,x,z);
                if(b.is(BiomeTags.IS_RIVER)||ocean==null&&b.is(BiomeTags.IS_OCEAN)){
                    int floor=level.getChunkSource().getGenerator().getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,level,level.getChunkSource().randomState())-1;
                    if(floor>=level.getSeaLevel()-1)continue;
                    var candidate=new Outlet(x,z,floor,true,mouth.x(),mouth.z(),nx,nz);
                    if(b.is(BiomeTags.IS_RIVER))return candidate;else ocean=candidate;
                }
            }
            return ocean!=null?ocean:new Outlet((int)endX,(int)endZ,level.getSeaLevel()-2,false,mouth.x(),mouth.z(),nx,nz);
        });
    }
    public static ChannelPoint position(Outlet outlet,double t){
        t=Math.clamp(t,0,1);double smooth=t*t*(3-2*t);
        double side=(outlet.x-outlet.edgeX)*-outlet.nz+(outlet.z-outlet.edgeZ)*outlet.nx;
        side=side*smooth+8*Math.sin(t*Math.PI*2)*Math.sin(t*Math.PI);
        return new ChannelPoint(outlet.edgeX+outlet.nx*GeyserTransition.WIDTH*t-outlet.nz*side,outlet.edgeZ+outlet.nz*GeyserTransition.WIDTH*t+outlet.nx*side);
    }
    private static double progress(Outlet outlet,int x,int z){return ((x-outlet.edgeX)*outlet.nx+(z-outlet.edgeZ)*outlet.nz)/GeyserTransition.WIDTH;}
    private record StageKey(long field,boolean north){}
    private static final Map<ServerLevel,Map<StageKey,int[]>> STAGES=new WeakHashMap<>();
    private static int channelLevel(ServerLevel level,GeyserRegion g,Outlet outlet,double t){
        Map<StageKey,int[]> cache;synchronized(STAGES){cache=STAGES.computeIfAbsent(level,l->new ConcurrentHashMap<>());}
        if(cache.size()>256)cache.clear();var route=GeyserDrainage.of(g);
        var stages=cache.computeIfAbsent(new StageKey(GeyserLedger.key(g),route.north()),key->{
            int count=GeyserTransition.WIDTH/16,sea=level.getSeaLevel();var values=new int[count+1];values[0]=Math.max(sea-1,route.mouth().water());
            for(int i=1;i<=count;i++){
                double t0=i/(double)count;var point=position(outlet,t0);int x=(int)Math.round(point.x),z=(int)Math.round(point.z);
                int nativeFloor=level.getChunkSource().getGenerator().getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,level,level.getChunkSource().randomState())-1;
                double d=GeyserTransition.distance(g,x,z),blend=GeyserTransition.progress(d,x,z);
                int ground=(int)Math.floor(GeyserTerrain.ground(g,x,z));
                int inland=GeyserTransition.floor(GeyserTransition.Climate.TEMPERATE,blend,ground,nativeFloor,sea),coast=GeyserTransition.floor(GeyserTransition.Climate.SEA,blend,ground,nativeFloor,sea);
                double ocean=Math.clamp((sea+10-nativeFloor)/24.0,0,1);ocean=ocean*ocean*(3-2*ocean);
                int reference=(int)Math.round(inland+(coast-inland)*ocean);
                double smooth=t0*t0*(3-2*t0);int grade=(int)Math.floor(values[0]*(1-smooth)+(sea-1)*smooth);
                values[i]=Math.min(values[i-1],Math.max(sea-1,Math.min(grade,reference-1)));
            }
            return values;
        });
        double p=Math.clamp(t,0,1)*(stages.length-1);int i=Math.min(stages.length-2,(int)p);
        return (int)Math.floor(stages[i]+(stages[i+1]-stages[i])*(p-i));
    }
    public static GeyserTerrain.Column channel(ServerLevel level,GeyserRegion g,int x,int z,int original){
        var dry=new GeyserTerrain.Column(original,Integer.MIN_VALUE,GeyserTerrain.WaterKind.NONE);var route=GeyserDrainage.of(g);
        if(!route.external())return dry;var outlet=outlet(level,g,route.north());double t=progress(outlet,x,z);
        if(t<0||t>=1||!outlet.connected)return dry;
        var center=position(outlet,t);double width=GeyserTerrain.riverWidth(g,(int)outlet.edgeZ)*(1-t)+6*t;
        double distance=Math.hypot(x-center.x,z-center.z);if(distance>width+28)return dry;
        int water=channelLevel(level,g,outlet,t);double smooth=t*t*(3-2*t),bed=(water-2)*(1-smooth)+outlet.floor*smooth;
        return GeyserTerrain.riverColumn(original,water,width,distance,Math.max(0,water-bed-1));
    }
    public static boolean river(ServerLevel level,GeyserRegion g,int x,int z){
        var route=GeyserDrainage.of(g);if(!route.external())return false;var outlet=outlet(level,g,route.north());
        double t=progress(outlet,x,z);if(t<0||t>1||!outlet.connected)return false;
        var center=position(outlet,t);return Math.hypot(x-center.x,z-center.z)<GeyserTerrain.riverWidth(g,(int)outlet.edgeZ)*(1-t)+6*t+2;
    }
    public static RiverCurrentField.Flow flow(ServerLevel level,GeyserRegion g,int x,int z){
        var route=GeyserDrainage.of(g);var outlet=outlet(level,g,route.north());double t=progress(outlet,x,z);
        var a=position(outlet,Math.max(0,t-.005));var b=position(outlet,Math.min(1,t+.005));
        double dx=b.x-a.x,dz=b.z-a.z,n=Math.hypot(dx,dz);return n==0?new RiverCurrentField.Flow(0,0):new RiverCurrentField.Flow(dx/n,dz/n);
    }
    private GeyserCoast(){}
}
