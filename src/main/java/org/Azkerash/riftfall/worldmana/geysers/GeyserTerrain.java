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
    public static double riverCenter(GeyserRegion g,double z){
        long h=waterSeed(g);double b=z-g.centerZ();
        double warp=b+85*(noise(h+8,b/290)-noise(h+8,0));
        return g.centerX()+260*(noise(h,warp/420)-noise(h,0))+95*(noise(h+1,warp/155)-noise(h+1,0))+24*(noise(h+2,warp/57)-noise(h+2,0));
    }
    public static double riverWidth(GeyserRegion g,double z){long h=waterSeed(g);double b=z-g.centerZ();return 4+5*noise(h+3,b/185)+2*noise(h+4,b/43);}
    public static double riverDistance(GeyserRegion g,int x,int z){return Math.abs(x-riverCenter(g,z));}
    /** One continuous route gives every connected lake only an inlet and an outlet, never a four-way crossing. */
    public static double junctionZ(GeyserRegion g){return g.centerZ()+g.innerRadiusChunks()*8.0;}
    public enum WaterKind { NONE, RIVER, LAKE }
    public record Column(int floor,int water,WaterKind kind) {
        public boolean wet(){return water>floor;}
    }
    /** The undisturbed local terrain; channels remove only a few blocks from this surface. */
    public static double ground(GeyserRegion g,double x,double z){
        double a=x-g.centerX(),b=z-g.centerZ();
        return 77+7*Math.sin(a/170)+5*Math.cos(b/210)+3*Math.sin((a+b)/87);
    }
    public static boolean openEnd(GeyserRegion g,boolean north){
        return (waterSeed(g)&(north?1:2))!=0;
    }
    private static double endZ(GeyserRegion g,boolean north){
        return g.centerZ()+(north?-1:1)*GeyserShape.radius(g)*.68;
    }
    private static int lakeLevel(GeyserRegion g,double lx,double lz,double radius){
        double low=ground(g,lx,lz);
        // Keep the pool below its surrounding shoreline so it cannot spill sideways over a low bank.
        for(int i=0;i<32;i++){
            double angle=i*Math.PI/16;
            low=Math.min(low,ground(g,lx+radius*1.25*Math.cos(angle),lz+radius*.77*1.25*Math.sin(angle)));
        }
        return (int)Math.floor(low)-1;
    }
    /** Long shallow reaches follow local relief, with occasional two-block rock steps. */
    public static int riverLevel(GeyserRegion g,double z){
        double level=ground(g,riverCenter(g,z),z)-1;
        double jz=junctionZ(g),blend=1-smooth(Math.clamp((Math.abs(z-jz)-110)/100,0,1));
        if(blend>0)level+=(lakeLevel(g,riverCenter(g,jz),jz,95+25*noise(waterSeed(g)+6,0))-level)*blend;
        for(boolean north:new boolean[]{false,true})if(!openEnd(g,north)){
            double ez=endZ(g,north),weight=1-smooth(Math.clamp((Math.abs(z-ez)-70)/90,0,1));
            if(weight>0)level+=(lakeLevel(g,riverCenter(g,ez),ez,85)-level)*weight;
        }
        int step=noise(waterSeed(g)+17,(z-g.centerZ())/180)>.8?2:1;
        return (int)Math.floor(level/step)*step;
    }
    private static Column basin(Column current,GeyserRegion g,int x,int z,double lx,double lz,double radius){
        double angle=Math.atan2(z-lz,x-lx);
        double edge=1+.12*Math.sin(angle*3)+.07*Math.sin(angle*5+1);
        double d=Math.hypot((x-lx)/radius,(z-lz)/(radius*.77))/edge;
        if(d>=1)return current;
        int water=riverLevel(g,lz);
        int terrain=(int)Math.floor(ground(g,x,z));
        // Keep high ground as natural peninsulas/islands rather than excavating a bowl.
        int floor=Math.min(current.floor,Math.max(terrain-4,(int)Math.floor(water-1-3*(1-d*d))));
        if(floor>=water)return current;
        return new Column(Math.min(current.floor,floor),water,WaterKind.LAKE);
    }
    public static Column column(long seed,GeyserRegion g,int x,int z){
        int terrain=(int)Math.floor(ground(g,x,z));
        Column result=new Column(terrain,Integer.MIN_VALUE,WaterKind.NONE);
        double width=riverWidth(g,z),offset=riverDistance(g,x,z)/width;
        boolean inRoute=(openEnd(g,true)||z>=endZ(g,true))&&(openEnd(g,false)||z<=endZ(g,false));
        if(inRoute&&offset<1.5){
            int level=riverLevel(g,z);
            double depth=1+2*noise(waterSeed(g)+5,(z-g.centerZ())/120);
            int bed=(int)Math.floor(level-1-depth*Math.max(0,1-offset*offset));
            double bank=smooth(Math.clamp((offset-1)/.5,0,1));
            int floor=(int)Math.floor(bed+(terrain-bed)*bank);
            result=new Column(floor,level,floor<level?WaterKind.RIVER:WaterKind.NONE);
        }
        double jz=junctionZ(g);
        result=basin(result,g,x,z,riverCenter(g,jz),jz,95+25*noise(waterSeed(g)+6,0));
        for(boolean north:new boolean[]{false,true})if(!openEnd(g,north)){
            double ez=endZ(g,north);result=basin(result,g,x,z,riverCenter(g,ez),ez,85);
        }
        // Additional sheltered lakes retain their local terrain instead of sea-level excavations.
        int cellX=Math.floorDiv(x,384),cellZ=Math.floorDiv(z,384);
        for(int i=cellX-1;i<=cellX+1;i++)for(int j=cellZ-1;j<=cellZ+1;j++){
            long h=GeyserLayout.mix(seed^i*731L^j*997L);if((h&3)!=0)continue;
            double lx=i*384+140+Math.floorMod(h>>>8,100),lz=j*384+140+Math.floorMod(h>>>16,100);
            double d=Math.hypot((x-lx)/55,(z-lz)/40);if(d>=1)continue;
            var zone=GeyserShape.zone(g,lx,lz);if(zone==GeyserZone.OUTER||zone==GeyserZone.OUTSIDE)continue;
            // Off-route lakes use the same local ground reference, not the distant river's height.
            int water=lakeLevel(g,lx,lz,55);
            int floor=Math.max(terrain-4,(int)Math.floor(water-1-2*(1-d*d)));
            if(d<1&&floor<water)result=new Column(Math.min(result.floor,floor),water,WaterKind.LAKE);
        }
        return result;
    }
    public static int height(long seed,GeyserRegion g,int x,int z){return column(seed,g,x,z).floor;}
    /** Coherent sediment patches with small-scale variation, extending several blocks into the bed. */
    public static BlockState sediment(long seed,int x,int z,int depth){
        long patch=GeyserLayout.mix(seed^Math.floorDiv(x,7)*731L^Math.floorDiv(z,7)*997L);
        int value=(int)Math.floorMod(patch+depth*3,12);
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
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int value=plan[x*16+z];if(value==Integer.MIN_VALUE||(value&1)==0)continue;
            int floor=value>>1,wx=cx*16+x,wz=cz*16+z;
            int water=plan.length==512?plan[256+x*16+z]:world.getSeaLevel()-1;
            for(int y=Math.max(world.getMinBuildHeight()+5,floor-4);y<=water;y++){
                pos.set(wx,y,wz);world.setBlock(pos,y>floor?Blocks.WATER.defaultBlockState():sediment(seed,wx,wz,floor-y),2);
            }
        }
        // A thin falling-water curtain bridges reach drops without raising the whole lower pool.
        var field=GeyserBiomes.field(world.getLevel(),cx,cz);
        if(field!=null&&plan.length==512)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int index=x*16+z;if(plan[index]==Integer.MIN_VALUE||(plan[index]&1)==0)continue;
            int wx=cx*16+x,wz=cz*16+z,water=plan[256+index];
            if(GeyserTransition.distance(field,wx,wz)>-4)continue;
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
