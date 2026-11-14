package org.Azkerash.riftfall.flora;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
/** Large, partly buried irregular stone masses, placed through ordinary biome decoration. */
public final class StarweaveBoulderFeature extends Feature<NoneFeatureConfiguration> {
 public StarweaveBoulderFeature(){super(NoneFeatureConfiguration.CODEC);}
 @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
  var level=context.level();var random=context.random();var origin=context.origin();
  int x=origin.getX(),z=origin.getZ(),ground=level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
  var floor=level.getBlockState(new BlockPos(x,ground,z));if(!floor.is(BlockTags.DIRT)&&!floor.is(ManaheartFlora.MOSS.get()))return false;
  int rx=3+random.nextInt(2),rz=2+random.nextInt(3),ry=2+random.nextInt(2);double center=ground+.4;
  for(int dx:new int[]{-rx,rx})for(int dz:new int[]{-rz,rz})if(Math.abs(level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x+dx,z+dz)-1-ground)>4)return false;
  boolean placed=false;double phase=random.nextDouble()*6.28;
  for(var p:BlockPos.betweenClosed(new BlockPos(x-rx-1,ground-ry,z-rz-1),new BlockPos(x+rx+1,ground+ry+1,z+rz+1))){
   double a=(p.getX()-x)/(rx+.35),b=(p.getY()-center)/(ry+.35),c=(p.getZ()-z)/(rz+.35);
   double edge=1+.11*Math.sin(p.getX()*1.3+p.getY()*.7+phase)*Math.cos(p.getZ()*1.1-phase);
   if(a*a+b*b+c*c>edge)continue;var old=level.getBlockState(p);
   if(!old.getFluidState().isEmpty()||level.getBlockEntity(p)!=null)continue;
   if(old.isAir()||old.is(BlockTags.DIRT)||old.is(BlockTags.BASE_STONE_OVERWORLD)||old.is(BlockTags.REPLACEABLE_BY_TREES)||old.is(ManaheartFlora.MOSS.get())||old.is(ManaheartFlora.CARPET.get())){level.setBlock(p,ManaheartFlora.BOULDER.get().defaultBlockState(),2);placed=true;}
  }
  return placed;
 }
}
