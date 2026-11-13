package org.Azkerash.riftfall.worldmana.geysers;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
@EventBusSubscriber(modid="riftfall")
public final class ShallowWaterVerification {
 static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
 @SubscribeEvent public static void run(ServerStartedEvent event){
  var level=event.getServer().overworld();GeyserRegion g=null;
  for(int cell=410;cell<450&&g==null;cell++)g=GeyserRegions.get(level).candidate(cell,4).filter(GeyserRegion::circular).orElse(null);
  require(g!=null,"No circular field");long seed=GeyserLayout.mix(level.getSeed()^0x474559534552L);
  for(int dz:new int[]{-300,0,(int)(GeyserTerrain.junctionZ(g)-g.centerZ())}){
   int z=(int)g.centerZ()+dz,x=(int)Math.round(GeyserTerrain.riverCenter(g,z));
   var chunk=level.getChunkSource().getChunk(x>>4,z>>4,ChunkStatus.CARVERS,true);
   var c=GeyserTerrain.column(seed,g,x,z);require(c.wet(),"Dry expected water");
   for(int y=c.floor()+1;y<=c.water();y++)require(chunk.getFluidState(new BlockPos(x,y,z)).is(FluidTags.WATER),"Water gap at "+x+","+y+","+z);
   require(!chunk.getBlockState(new BlockPos(x,c.floor(),z)).isAir(),"Unsealed bed");
   // Biomes are four-block cells; compare their sampled centers, not an arbitrary column's edge.
   for(int a=0;a<4;a++)for(int b=0;b<4;b++){
    int bx=(x>>4)*16+a*4+2,bz=(z>>4)*16+b*4+2;
    require(chunk.getNoiseBiome(bx>>2,20,bz>>2).is(GeyserBiomes.surfaceKey(seed,g,bx,bz)),"Wrong aquatic biome");
   }
   System.out.println("SHALLOW_WATER_PASS x="+x+" z="+z+" floor="+c.floor()+" water="+c.water()+" kind="+c.kind());
  }
  boolean testedDrop=false;
  for(int dz=-600;dz<300&&!testedDrop;dz++){
   int zz=(int)g.centerZ()+dz,xx=(int)Math.round(GeyserTerrain.riverCenter(g,zz));
   var low=GeyserTerrain.column(seed,g,xx,zz);var high=GeyserTerrain.column(seed,g,xx,zz-1);
   if(!low.wet()||!high.wet()||high.water()<=low.water()||high.floor()>=low.water())continue;
   var chunk=level.getChunkSource().getChunk(xx>>4,zz>>4,ChunkStatus.CARVERS,true);
   require(chunk.getFluidState(new BlockPos(xx,low.water()+1,zz)).is(FluidTags.WATER),"Disconnected cascade");
   testedDrop=true;System.out.println("CASCADE_PASS x="+xx+" z="+zz+" from="+high.water()+" to="+low.water());
  }
  require(testedDrop,"No cascade sampled");
  boolean testedOutlet=false;
  for(int cell=410;cell<450&&!testedOutlet;cell++){
   var candidate=GeyserRegions.get(level).candidate(cell,4);if(candidate.isEmpty())continue;var field=candidate.orElseThrow();
   for(boolean north:new boolean[]{false,true}){
    if(testedOutlet||!GeyserTerrain.openEnd(field,north))continue;
    var outlet=GeyserCoast.outlet(level,field,north);if(!outlet.connected())continue;
    int previous=999;
    for(int distance:new int[]{1,128,256,383}){
     int zz=outlet.edgeZ()+(north?-1:1)*distance,xx=(int)Math.round(GeyserCoast.channelCenter(level,field,zz));
     var chunk=level.getChunkSource().getChunk(xx>>4,zz>>4,ChunkStatus.CARVERS,true);
     int top=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,xx&15,zz&15);
     require(chunk.getFluidState(new BlockPos(xx,top,zz)).is(FluidTags.WATER),"Dry external connection at "+distance);
     require(top<=previous,"Uphill external connection");previous=top;
     System.out.println("OUTLET_PASS distance="+distance+" x="+xx+" z="+zz+" water="+top);
    }
    testedOutlet=true;
   }
  }
  require(testedOutlet,"No external connection tested");
  int x=(int)g.centerX(),z=(int)g.centerZ();level.getChunk(x>>4,z>>4);
  System.out.println("AQUATIC_DECORATION_PASS full chunk generated");event.getServer().halt(false);
 }
}
