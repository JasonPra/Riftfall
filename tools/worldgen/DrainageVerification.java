package org.Azkerash.riftfall.worldmana.geysers;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
/** Opt-in probe for a fresh disposable development world, seed 923475. Never ship in main sources. */
@EventBusSubscriber(modid="riftfall")
public final class DrainageVerification {
 static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
 @SubscribeEvent public static void run(ServerStartedEvent event){
  var level=event.getServer().overworld();GeyserRegion surface=null,sink=null;
  for(int cell=410;cell<480&&(surface==null||sink==null);cell++){
   var candidate=GeyserRegions.get(level).candidate(cell,4);if(candidate.isEmpty())continue;
   var g=candidate.orElseThrow();if(GeyserDrainage.of(g).underground())sink=g;else surface=g;
  }
  require(surface!=null&&sink!=null,"Missing drainage samples");long seed=GeyserLayout.mix(level.getSeed()^0x474559534552L);
  var route=GeyserDrainage.of(surface);int previous=999;
  for(int index:new int[]{0,route.path().size()/3,route.path().size()*2/3,route.path().size()-1}){
   var p=route.path().get(index);int x=(int)Math.round(p.x()),z=(int)Math.round(p.z());
   var c=GeyserTerrain.column(seed,surface,x,z);require(c.wet(),"Dry planned water");
   var chunk=level.getChunkSource().getChunk(x>>4,z>>4,ChunkStatus.CARVERS,true);
   int cap=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,x&15,z&15);
   int floor=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,x&15,z&15);
   require(chunk.getFluidState(new BlockPos(x,cap,z)).is(FluidTags.WATER),"Dry actual river surface");
   require(cap<=previous,"Actual river climbs");previous=cap;
   if(GeyserShape.distance(surface,x,z)<-1)require(cap==c.water(),"Interior water disagrees with route");
   // At the rounded boundary column, the exterior grade can already be below the interior stage.
   for(int y=floor+1;y<=cap;y++)require(chunk.getFluidState(new BlockPos(x,y,z)).is(FluidTags.WATER),"Actual water-column gap at "+x+","+y+","+z);
   require(!chunk.getBlockState(new BlockPos(x,floor,z)).isAir(),"Unsealed bed");
   var flow=route.flow(x,z);var reach=route.nearest(x,z);
   double tx=route.mouth().x()-x,tz=route.mouth().z()-z,d=Math.hypot(tx,tz);
   if(!route.external()&&d<48){if(d>1)require((flow.x()*tx+flow.z()*tz)/d>.8,"Current misses receiving pool");}
   else require(flow.x()*reach.flowX()+flow.z()*reach.flowZ()>.8,"Current reverses");
   System.out.println("DRAINAGE_SURFACE_PASS x="+x+" z="+z+" water="+cap+" kind="+c.kind());
  }
  boolean connected=false;
  for(int cell=410;cell<480&&!connected;cell++){
   var candidate=GeyserRegions.get(level).candidate(cell,4);if(candidate.isEmpty())continue;var field=candidate.orElseThrow();var planned=GeyserDrainage.of(field);
   if(!planned.external())continue;var outlet=GeyserCoast.outlet(level,field,planned.north());if(!outlet.connected())continue;
   int last=999;
   for(double t:new double[]{1/384.0,128/384.0,256/384.0,383/384.0}){
    var center=GeyserCoast.position(outlet,t);int x=(int)Math.round(center.x()),z=(int)Math.round(center.z());
    var chunk=level.getChunkSource().getChunk(x>>4,z>>4,ChunkStatus.CARVERS,true);
    int y=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,x&15,z&15);
    require(chunk.getFluidState(new BlockPos(x,y,z)).is(FluidTags.WATER),"Dry radial connection");require(y<=last,"Uphill radial connection");last=y;
    System.out.println("DRAINAGE_OUTLET_PASS x="+x+" z="+z+" water="+y);
   }
   connected=true;
  }
  require(connected,"No native outlet tested");
  var river=GeyserUndergroundRiver.of(seed,sink);require(river.active(),"No underground river");
  for(var p:river.path()){
   int x=(int)Math.round(p.x()),z=(int)Math.round(p.z());var cut=river.column(x,z);require(cut!=null,"Disconnected tunnel plan");
   var chunk=level.getChunkSource().getChunk(x>>4,z>>4,ChunkStatus.CARVERS,true);
   int water=Math.min(cut.water(),cut.roof());
   require(chunk.getFluidState(new BlockPos(x,water,z)).is(FluidTags.WATER),"Dry underground channel at "+x+","+water+","+z);
   if(!cut.falling())require(chunk.getNoiseBiome(x>>2,water>>2,z>>2).is(GeyserBiomes.waterKey(GeyserShape.zone(sink,x,z),false)),"Missing underground river biome");
   if(cut.falling())for(int y=cut.floor()+1;y<=cut.water();y++)require(chunk.getFluidState(new BlockPos(x,y,z)).is(FluidTags.WATER),"Plugged sinkhole at "+y);
   System.out.println("DRAINAGE_UNDERGROUND_PASS x="+x+" z="+z+" floor="+cut.floor()+" water="+water+" shaft="+cut.falling());
  }
  var mouth=GeyserDrainage.of(sink).mouth();level.getChunk((int)mouth.x()>>4,(int)mouth.z()>>4);
  System.out.println("DRAINAGE_DECORATION_PASS full sinkhole chunk generated");event.getServer().halt(false);
 }
}
