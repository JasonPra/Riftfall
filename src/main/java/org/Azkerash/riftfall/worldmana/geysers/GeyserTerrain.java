package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Gentle wooded terrain, connected valley rivers and irregular sheltered lake basins. */
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
    public static double riverWidth(GeyserRegion g,double z){long h=waterSeed(g);double b=z-g.centerZ();return 6+11*noise(h+3,b/185)+4*noise(h+4,b/43);}
    public static double riverDistance(GeyserRegion g,int x,int z){return Math.abs(x-riverCenter(g,z));}
    /** One continuous route gives every connected lake only an inlet and an outlet, never a four-way crossing. */
    public static double junctionZ(GeyserRegion g){return g.centerZ()+g.innerRadiusChunks()*8.0;}
    private static double lake(long seed,double x,double z,double lx,double lz,double radius){
        double angle=Math.atan2(z-lz,x-lx);
        double edge=1+.13*Math.sin(angle*3+(seed&31))+.08*Math.sin(angle*5)+.05*Math.sin(x/17+z/23);
        double d=Math.sqrt(sq((x-lx)/radius)+sq((z-lz)/(radius*.77)))/edge;
        return d<1.65?51+31*sq(Math.max(0,d-.25)/1.4):999;
    }
    public static int height(long seed,GeyserRegion g,int x,int z){
        double a=x-g.centerX(),b=z-g.centerZ(),river=riverDistance(g,x,z),width=riverWidth(g,z);
        double hills=77+7*Math.sin(a/170)+5*Math.cos(b/210)+3*Math.sin((a+b)/87);
        double normalized=river/width,depth=2+3*noise(waterSeed(g)+5,b/120);
        double bed=60-depth*Math.max(0,1-normalized*normalized);
        double valley=smooth(Math.clamp((normalized-1)/1.8,0,1));
        double height=normalized<=1?bed:60+(hills-60)*valley;
        // The former crossing becomes a broad irregular lake with exactly two channel connections.
        double jz=junctionZ(g),jx=riverCenter(g,jz);
        height=Math.min(height,lake(waterSeed(g),x,z,jx,jz,95+25*noise(waterSeed(g)+6,0)));
        // Scattered irregular lakes, not circular excavations or raised rims.
        int cellX=Math.floorDiv(x,384),cellZ=Math.floorDiv(z,384);
        for(int i=cellX-1;i<=cellX+1;i++)for(int j=cellZ-1;j<=cellZ+1;j++){
            long h=GeyserLayout.mix(seed^i*731L^j*997L);
            if((h&3)!=0)continue;
            double lx=i*384+140+Math.floorMod(h>>>8,100),lz=j*384+140+Math.floorMod(h>>>16,100);
            var zone=g.zone((int)lx>>4,(int)lz>>4);if(zone==GeyserZone.OUTER||zone==GeyserZone.OUTSIDE)continue;
            double radius=zone==GeyserZone.INNER?55:35;
            double distance=Math.sqrt(sq((x-lx)/radius)+sq((z-lz)/(radius*.72)));
            distance+=.1*Math.sin(x/13.0)+.08*Math.sin(z/17.0);
            if(distance<1.6)height=Math.min(height,58+22*Math.pow(Math.max(0,distance-.5)/1.1,2));
        }
        // Only some cores contain a wooded water basin; the outline is asymmetric and rippled.
        long core=GeyserLayout.mix(seed^g.centerChunkX()*997L^g.centerChunkZ()*731L);
        if((core&3)==0){
            double d=Math.sqrt(sq((a-25)/48)+sq((b+18)/35))+.1*Math.sin(a/11)+.08*Math.cos(b/9);
            if(d<1.6)height=Math.min(height,57+24*sq(Math.max(0,d-.45)/1.15));
        }
        if(height<63)height+=.65*Math.sin(x/11.0)+.45*Math.cos(z/17.0)+.25*Math.sin((x+z)/7.0)-.4;
        return (int)Math.floor(height);
    }
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
        var pos=new BlockPos.MutableBlockPos();var plan=new int[256];java.util.Arrays.fill(plan,Integer.MIN_VALUE);
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            int oldFloor=world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1,oldTop=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
            double distance=GeyserTransition.distance(g,x,z);
            if(distance>=GeyserTransition.WIDTH)continue;
            int sea=world.getSeaLevel(),waterTop=sea-1,custom=height(seed,g,x,z),floor=custom;
            if(distance>0){
                double t=GeyserTransition.progress(distance,x,z);
                int inland=GeyserTransition.floor(GeyserTransition.Climate.TEMPERATE,t,custom,oldFloor,sea);
                int coast=GeyserTransition.floor(GeyserTransition.Climate.SEA,t,custom,oldFloor,sea);
                double ocean=Math.clamp((sea+10-oldFloor)/24.0,0,1);ocean=ocean*ocean*(3-2*ocean);
                floor=(int)Math.round(inland+(coast-inland)*ocean);
                floor=Math.min(floor,GeyserCoast.channelBed(world.getLevel(),g,x,z,floor));
            }
            var originalTop=world.getBlockState(new BlockPos(x,oldFloor,z));
            var surfaceBiome=world.getBiome(new BlockPos(x,floor+1,z));
            boolean sandy=surfaceBiome.is(net.minecraft.tags.BiomeTags.IS_BEACH)||surfaceBiome.is(net.minecraft.world.level.biome.Biomes.DESERT);
            var dryTop=distance>GeyserTransition.WIDTH*.85?originalTop:sandy?Blocks.SAND.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState();
            int water=floor<waterTop?waterTop:Integer.MIN_VALUE;
            plan[(x&15)*16+(z&15)]=(floor<<1)|(water>floor?1:0);
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
        if(plan==null||plan.length!=256)return;
        var pos=new BlockPos.MutableBlockPos();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int value=plan[x*16+z];if(value==Integer.MIN_VALUE||(value&1)==0)continue;
            int floor=value>>1,wx=cx*16+x,wz=cz*16+z;
            for(int y=Math.max(world.getMinBuildHeight()+5,floor-4);y<world.getSeaLevel();y++){
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
            if(top<69||riverDistance(g,(int)ex,(int)ez)<riverWidth(g,ez)*2+20)continue;
            double cy=start.y()+(top+2-start.y())*t;
            return new Entrance(cy,Math.sqrt(9-horizontal));
        }
        return null;
    }
    public static boolean entrance(long seed,GeyserRegion g,int x,int y,int z){var e=entranceColumn(seed,g,x,z);return e!=null&&e.contains(y);}
    private GeyserTerrain(){}
}
