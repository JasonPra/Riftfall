package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Wooded terrain with shallow terrain-following waterways and sheltered lakes. */
public final class GeyserTerrain {
    private static double sq(double v){return v*v;}
    private static long waterSeed(GeyserRegion g){return GeyserLayout.mix(g.centerChunkX()*731L^g.centerChunkZ()*997L);}
    private static double smooth(double t){return t*t*(3-2*t);}
    private static double noise(long seed,double coordinate){
        int cell=(int)Math.floor(coordinate);double t=smooth(coordinate-cell);
        double a=(GeyserLayout.mix(seed^cell*731L)>>>11)*0x1.0p-53,b=(GeyserLayout.mix(seed^(cell+1)*731L)>>>11)*0x1.0p-53;
        return a+(b-a)*t;
    }
    public static double riverCenter(GeyserRegion g,double z){return GeyserDrainage.of(g).centerAt(z);}
    public static double riverWidth(GeyserRegion g,double z){long h=waterSeed(g);double b=z-g.centerZ();return 4+5*noise(h+3,b/185)+2*noise(h+4,b/43);}
    public static double riverDistance(GeyserRegion g,int x,int z){return GeyserDrainage.of(g).nearest(x,z).distance();}
    public static double junctionZ(GeyserRegion g){return GeyserDrainage.of(g).mouth().z();}
    public enum WaterKind { NONE, RIVER, LAKE }
    public record Column(int floor,int water,WaterKind kind) {
        public boolean wet(){return water>floor;}
    }
    public static double ground(GeyserRegion g,double x,double z){
        double a=x-g.centerX(),b=z-g.centerZ();
        return 77+7*Math.sin(a/170)+5*Math.cos(b/210)+3*Math.sin((a+b)/87);
    }
    public static boolean openEnd(GeyserRegion g,boolean north){var route=GeyserDrainage.of(g);return route.external()&&route.north()==north;}
    public static int riverLevel(GeyserRegion g,double z){
        var route=GeyserDrainage.of(g);var nearest=route.nearest(route.centerAt(z),z);
        return nearest.segment()>=0?nearest.water():route.mouth().water();
    }
    private static double shoreGround(GeyserRegion g,int x,int z){
        return (ground(g,x,z)*4+ground(g,x-12,z)+ground(g,x+12,z)+ground(g,x,z-12)+ground(g,x,z+12))/8;
    }
    /** A spring/receiving pool grades through a broad shallow shelf into the existing ground. */
    private static Column pool(Column current,GeyserRegion g,int x,int z,GeyserDrainage.Point center,double radius){
        double angle=Math.atan2(z-center.z(),x-center.x());
        double d=Math.hypot(x-center.x(),(z-center.z())/ .85)/(1+.06*Math.sin(angle*3));
        if(d>=radius+32)return current;
        double bank=smooth(Math.clamp((d-radius)/32,0,1));
        double bed=center.water()-1-2*Math.max(0,1-sq(d/radius));
        int floor=(int)Math.floor(bed+(shoreGround(g,x,z)-bed)*bank);
        if(floor>=center.water())return new Column(Math.min(current.floor(),floor),current.water(),current.kind());
        return new Column(Math.min(current.floor(),floor),center.water(),WaterKind.LAKE);
    }
    public static Column column(long seed,GeyserRegion g,int x,int z){
        var route=GeyserDrainage.of(g);var reach=route.nearest(x,z);
        int terrain=(int)Math.floor(ground(g,x,z));var result=new Column(terrain,Integer.MIN_VALUE,WaterKind.NONE);
        double width=riverWidth(g,z),distance=reach.distance();
        if(distance<width+32){
            double offset=distance/width,depth=1+2*noise(waterSeed(g)+5,reach.segment()/8.0);
            double bed=reach.water()-1-depth*Math.max(0,1-offset*offset);
            double bank=smooth(Math.clamp((distance-width)/32,0,1));
            int floor=(int)Math.floor(bed+(shoreGround(g,x,z)-bed)*bank);
            result=new Column(floor,reach.water(),floor<reach.water()?WaterKind.RIVER:WaterKind.NONE);
        }
        int lake=route.lakeAt(x,z);
        if(lake!=Integer.MIN_VALUE){
            // Natural flooded depressions use their real ground as bed: no hard ellipse cutoff.
            int floor=(int)Math.floor(shoreGround(g,x,z));
            if(floor<lake)result=new Column(Math.min(result.floor(),floor),lake,WaterKind.LAKE);
        }
        result=pool(result,g,x,z,route.source(),14);
        if(!route.external())result=pool(result,g,x,z,route.mouth(),route.underground()?24:36);
        return result;
    }
    public static int height(long seed,GeyserRegion g,int x,int z){return column(seed,g,x,z).floor;}
    /** Coherent sediment patches with small-scale variation, extending several blocks into the bed. */
    public static BlockState sediment(long seed,int x,int z,int depth){
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
        var pos=new BlockPos.MutableBlockPos();var plan=new int[512];java.util.Arrays.fill(plan,Integer.MIN_VALUE);
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            int oldFloor=world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1,oldTop=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
            double distance=GeyserTransition.distance(g,x,z);
            if(distance>=GeyserTransition.WIDTH)continue;
            int sea=world.getSeaLevel();var sample=column(seed,g,x,z);
            int waterTop=sample.water,custom=sample.floor,floor=custom;
            if(distance>0){
                double t=GeyserTransition.progress(distance,x,z);
                int inland=GeyserTransition.floor(GeyserTransition.Climate.TEMPERATE,t,custom,oldFloor,sea);
                int coast=GeyserTransition.floor(GeyserTransition.Climate.SEA,t,custom,oldFloor,sea);
                double ocean=Math.clamp((sea+10-oldFloor)/24.0,0,1);ocean=ocean*ocean*(3-2*ocean);
                floor=(int)Math.round(inland+(coast-inland)*ocean);
                var channel=GeyserCoast.channel(world.getLevel(),g,x,z,floor);
                floor=channel.floor();waterTop=channel.wet()?channel.water():floor<sea-1?sea-1:Integer.MIN_VALUE;
            }
            var originalTop=world.getBlockState(new BlockPos(x,oldFloor,z));
            var surfaceBiome=world.getBiome(new BlockPos(x,floor+1,z));
            boolean sandy=surfaceBiome.is(net.minecraft.tags.BiomeTags.IS_BEACH)||surfaceBiome.is(net.minecraft.world.level.biome.Biomes.DESERT);
            var dryTop=distance>GeyserTransition.WIDTH*.85?originalTop:sandy?Blocks.SAND.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState();
            int water=floor<waterTop?waterTop:Integer.MIN_VALUE;
            int index=(x&15)*16+(z&15);
            plan[index]=(floor<<1)|(water>floor?1:0);plan[256+index]=water;
            int top=Math.min(world.getMaxBuildHeight()-1,Math.max(Math.max(oldTop,floor+1),water));
            for(int y=Math.max(world.getMinBuildHeight()+5,Math.min(oldFloor,floor)-4);y<=top;y++){
                pos.set(x,y,z);if(!natural(world.getBlockState(pos)))continue;
                BlockState state=y>floor?(y<=water?Blocks.WATER:Blocks.AIR).defaultBlockState():water>floor&&y>=floor-3?sediment(seed,x,z,floor-y):y==floor?dryTop:y>=floor-3?(sandy?Blocks.SAND:Blocks.DIRT).defaultBlockState():(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
                world.setBlock(pos,state,2);
            }
        }
        world.getChunk(cx,cz).setData(GeyserSurfacePlan.PLAN,plan);
    }
    /** Restore authoritative water and its sediment seal after vanilla/custom carving, before decoration. */
    public static void sealWater(WorldGenLevel world,int cx,int cz,long seed){
        var chunk=world.getChunk(cx,cz);var plan=chunk.getExistingDataOrNull(GeyserSurfacePlan.PLAN);
        if(plan==null||(plan.length!=256&&plan.length!=512))return;
        var pos=new BlockPos.MutableBlockPos();
        var field=GeyserBiomes.field(world.getLevel(),cx,cz);
        var underground=field==null?null:GeyserUndergroundRiver.of(seed,field);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int value=plan[x*16+z];if(value==Integer.MIN_VALUE||(value&1)==0)continue;
            int floor=value>>1,wx=cx*16+x,wz=cz*16+z;
            int water=plan.length==512?plan[256+x*16+z]:world.getSeaLevel()-1;
            if(underground!=null&&underground.mouth(wx,wz))continue;
            for(int y=Math.max(world.getMinBuildHeight()+5,floor-4);y<=water;y++){
                pos.set(wx,y,wz);world.setBlock(pos,y>floor?Blocks.WATER.defaultBlockState():sediment(seed,wx,wz,floor-y),2);
            }
        }
        // A thin falling-water curtain bridges reach drops without raising the whole lower pool.
        if(field!=null&&plan.length==512)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int index=x*16+z;if(plan[index]==Integer.MIN_VALUE||(plan[index]&1)==0)continue;
            int wx=cx*16+x,wz=cz*16+z,water=plan[256+index];
            if(GeyserTransition.distance(field,wx,wz)>-4||underground!=null&&underground.mouth(wx,wz))continue;
            int spill=water;
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){
                var neighbor=column(seed,field,wx+direction.getStepX(),wz+direction.getStepZ());
                if(neighbor.wet()&&neighbor.floor()<water)spill=Math.max(spill,neighbor.water());
            }
            for(int y=water+1;y<=Math.min(water+3,spill);y++){
                pos.set(wx,y,wz);
                if(!world.getBlockState(pos).isAir())break;
                world.setBlock(pos,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8),2);
                chunk.markPosForPostprocessing(pos);
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
            if(top<69||riverDistance(g,(int)ex,(int)ez)<riverWidth(g,ez)*2+20)continue;
            double cy=start.y()+(top+2-start.y())*t;
            return new Entrance(cy,Math.sqrt(9-horizontal));
        }
        return null;
    }
    public static boolean entrance(long seed,GeyserRegion g,int x,int y,int z){var e=entranceColumn(seed,g,x,z);return e!=null&&e.contains(y);}
    private GeyserTerrain(){}
}
