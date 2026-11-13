package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Existing wooded land profile and outer coast transition; no custom inland waterways. */
public final class GeyserTerrain {
    private static double sq(double v){return v*v;}
    private static double smooth(double t){return t*t*(3-2*t);}
    private static double noise(long seed,double coordinate){
        int cell=(int)Math.floor(coordinate);double t=smooth(coordinate-cell);
        double a=(GeyserLayout.mix(seed^cell*731L)>>>11)*0x1.0p-53,b=(GeyserLayout.mix(seed^(cell+1)*731L)>>>11)*0x1.0p-53;
        return a+(b-a)*t;
    }
    /** Existing land elevations, unchanged while the next terrain design is on hold. */
    public static double ground(GeyserRegion g,double x,double z){
        double a=x-g.centerX(),b=z-g.centerZ();
        return 77+7*Math.sin(a/170)+5*Math.cos(b/210)+3*Math.sin((a+b)/87);
    }
    public static int height(long seed,GeyserRegion g,int x,int z){return (int)Math.floor(ground(g,x,z));}
    /** Sediment used only where the existing outer transition meets ocean water. */
    private static BlockState sediment(long seed,int x,int z,int depth){
        int row=Math.floorDiv(z,31);double t=smooth(Math.floorMod(z,31)/31.0);
        double a=noise(seed^row*997L,x/37.0),b=noise(seed^(row+1)*997L,x/37.0);
        int value=(int)Math.floor((a+(b-a)*t)*12+depth*.7)%12;
        return (value<4?Blocks.GRAVEL:value<7?Blocks.SAND:value<9?Blocks.CLAY:value<11?Blocks.MUD:Blocks.COARSE_DIRT).defaultBlockState();
    }
    private static boolean natural(BlockState s){
        return s.isAir()||!s.getFluidState().isEmpty()||s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)
            ||s.is(BlockTags.TERRACOTTA)||s.is(Blocks.GRAVEL)||s.is(Blocks.SNOW)||s.is(Blocks.SNOW_BLOCK)||s.is(BlockTags.ICE)
            ||s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
    }
    public static void reshape(WorldGenLevel world,GeyserRegion g,long seed,int cx,int cz){
        var pos=new BlockPos.MutableBlockPos();var plan=new int[256];java.util.Arrays.fill(plan,Integer.MIN_VALUE);
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            int oldFloor=world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1,oldTop=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
            double distance=GeyserTransition.distance(g,x,z);
            if(distance>=GeyserTransition.WIDTH)continue;
            int sea=world.getSeaLevel(),custom=height(seed,g,x,z),floor=custom;
            if(distance>0){
                double t=GeyserTransition.progress(distance,x,z);
                int inland=GeyserTransition.floor(GeyserTransition.Climate.TEMPERATE,t,custom,oldFloor,sea);
                int coast=GeyserTransition.floor(GeyserTransition.Climate.SEA,t,custom,oldFloor,sea);
                double ocean=Math.clamp((sea+10-oldFloor)/24.0,0,1);ocean=ocean*ocean*(3-2*ocean);
                floor=(int)Math.round(inland+(coast-inland)*ocean);
            }
            var originalTop=world.getBlockState(new BlockPos(x,oldFloor,z));
            var surfaceBiome=world.getBiome(new BlockPos(x,floor+1,z));
            boolean sandy=surfaceBiome.is(net.minecraft.tags.BiomeTags.IS_BEACH)||surfaceBiome.is(net.minecraft.world.level.biome.Biomes.DESERT);
            var dryTop=distance>GeyserTransition.WIDTH*.85?originalTop:sandy?Blocks.SAND.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState();
            int water=distance>0&&floor<sea-1?sea-1:Integer.MIN_VALUE;
            int index=(x&15)*16+(z&15);
            plan[index]=(floor<<1)|(water>floor?1:0);
            int top=Math.min(world.getMaxBuildHeight()-1,Math.max(Math.max(oldTop,floor+1),water));
            for(int y=Math.max(world.getMinBuildHeight()+5,Math.min(oldFloor,floor)-4);y<=top;y++){
                pos.set(x,y,z);if(!natural(world.getBlockState(pos)))continue;
                BlockState state=y>floor?(y<=water?Blocks.WATER:Blocks.AIR).defaultBlockState():water>floor&&y>=floor-3?sediment(seed,x,z,floor-y):y==floor?dryTop:y>=floor-3?(sandy?Blocks.SAND:Blocks.DIRT).defaultBlockState():(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
                world.setBlock(pos,state,2);
            }
        }
        world.getChunk(cx,cz).setData(GeyserSurfacePlan.PLAN,plan);
    }
    /** Finish the outer ocean transition and release the temporary cave-roof plan. */
    public static void finishSurface(WorldGenLevel world,int cx,int cz,long seed){
        var chunk=world.getChunk(cx,cz);var plan=chunk.getExistingDataOrNull(GeyserSurfacePlan.PLAN);
        if(plan==null||(plan.length!=256&&plan.length!=512))return;
        var pos=new BlockPos.MutableBlockPos();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int value=plan[x*16+z];if(value==Integer.MIN_VALUE||(value&1)==0)continue;
            int floor=value>>1,wx=cx*16+x,wz=cz*16+z;
            int water=plan.length==512?plan[256+x*16+z]:world.getSeaLevel()-1;
            for(int y=Math.max(world.getMinBuildHeight()+5,floor-4);y<=water;y++){
                pos.set(wx,y,wz);world.setBlock(pos,y>floor?Blocks.WATER.defaultBlockState():sediment(seed,wx,wz,floor-y),2);
            }
        }
        chunk.removeData(GeyserSurfacePlan.PLAN);
    }
    public record Entrance(double centerY,double height){public boolean contains(int y){return Math.abs(y-centerY)<height;}}
    /** Sparse ramps start on actual upper cave junctions rather than disconnected fixed-grid holes. */
    public static Entrance entranceColumn(long seed,GeyserRegion g,int x,int z){
        int cellX=Math.floorDiv(x,GeyserCaveShape.SPACING),cellZ=Math.floorDiv(z,GeyserCaveShape.SPACING);
        for(int a=cellX-2;a<=cellX+1;a++)for(int b=cellZ-1;b<=cellZ+1;b++){
            var start=GeyserCaveShape.node(seed,a,0,b);if((start.hash()&31)!=7)continue;
            double ex=start.x()+80,ez=start.z()+Math.floorMod(start.hash()>>>16,25)-12;
            double dx=ex-start.x(),dz=ez-start.z(),t=Math.clamp(((x-start.x())*dx+(z-start.z())*dz)/(dx*dx+dz*dz),0,1);
            double horizontal=sq(x-(start.x()+t*dx))+sq(z-(start.z()+t*dz));if(horizontal>=9)continue;
            double limit=g.halfWidth()*16.0-192;
            if(Math.max(Math.abs(start.x()-g.centerX()),Math.abs(start.z()-g.centerZ()))>limit||Math.max(Math.abs(ex-g.centerX()),Math.abs(ez-g.centerZ()))>limit)continue;
            if(start.y()+3>height(seed,g,(int)start.x(),(int)start.z())-24)continue;
            int top=height(seed,g,(int)ex,(int)ez);
            if(top<69)continue;
            double cy=start.y()+(top+2-start.y())*t;
            return new Entrance(cy,Math.sqrt(9-horizontal));
        }
        return null;
    }
    public static boolean entrance(long seed,GeyserRegion g,int x,int y,int z){var e=entranceColumn(seed,g,x,z);return e!=null&&e.contains(y);}
    private GeyserTerrain(){}
}
