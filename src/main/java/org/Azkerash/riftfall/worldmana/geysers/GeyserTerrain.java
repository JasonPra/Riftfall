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
    public static double riverDistance(GeyserRegion g,int x,int z){
        double a=x-g.centerX(),b=z-g.centerZ();
        return Math.min(Math.abs(a-75*Math.sin(b/210)-17*Math.sin(b/71)),Math.abs(b-90*Math.sin(a/250)-13*Math.sin(a/83)));
    }
    public static int height(long seed,GeyserRegion g,int x,int z){
        double a=x-g.centerX(),b=z-g.centerZ(),river=riverDistance(g,x,z);
        double hills=77+7*Math.sin(a/170)+5*Math.cos(b/210)+3*Math.sin((a+b)/87);
        double valley=Math.clamp(river/65,0,1);valley=valley*valley*(3-2*valley);
        double height=61+(hills-61)*valley;
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
        return (int)Math.floor(height);
    }
    private static boolean natural(BlockState s){
        return s.isAir()||!s.getFluidState().isEmpty()||s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)
            ||s.is(BlockTags.TERRACOTTA)||s.is(Blocks.GRAVEL)||s.is(Blocks.SNOW)||s.is(Blocks.SNOW_BLOCK)||s.is(BlockTags.ICE)
            ||s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
    }
    public static void reshape(WorldGenLevel world,GeyserRegion g,long seed,int cx,int cz){
        var pos=new BlockPos.MutableBlockPos();
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            int oldFloor=world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1,oldTop=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
            double edge=Math.min(Math.min(x-(g.centerChunkX()-g.halfWidth())*16,(g.centerChunkX()+g.halfWidth()+1)*16-1-x),Math.min(z-(g.centerChunkZ()-g.halfWidth())*16,(g.centerChunkZ()+g.halfWidth()+1)*16-1-z));
            double mix=Math.clamp(edge/192,0,1);mix=mix*mix*(3-2*mix);
            if(mix==0)continue;
            int floor=(int)Math.round(oldFloor*(1-mix)+height(seed,g,x,z)*mix);
            int water=floor<63&&(mix>.95||oldTop>=63)?63:Integer.MIN_VALUE;
            int top=Math.min(world.getMaxBuildHeight()-1,Math.max(Math.max(oldTop,floor+1),water));
            for(int y=Math.max(world.getMinBuildHeight()+5,Math.min(oldFloor,floor)-4);y<=top;y++){
                pos.set(x,y,z);if(!natural(world.getBlockState(pos)))continue;
                BlockState state=y>floor?(y<=water?Blocks.WATER:Blocks.AIR).defaultBlockState():y==floor?(water>floor?Blocks.SAND:Blocks.GRASS_BLOCK).defaultBlockState():y>=floor-3?Blocks.DIRT.defaultBlockState():(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
                world.setBlock(pos,state,2);
            }
        }
    }
    /** Rare narrow sloping entrances. Grid-aligned starts join the stacked junction shafts. */
    public static boolean entrance(long seed,GeyserRegion g,int x,int y,int z){
        int cx=Math.floorDiv(x-64,256),cz=Math.floorDiv(z-64,256);
        long hash=GeyserLayout.mix(seed^cx*0x632be59bd9b4e019L^cz*0x9e3779b97f4a7c15L);
        if((hash&7)!=0)return false;
        int startX=cx*256+64,startZ=cz*256+64;
        int endHeight=height(seed,g,startX+48,startZ);
        if(riverDistance(g,startX+48,startZ)<65||endHeight<69)return false;
        double t=Math.clamp((x-startX)/48.0,0,1),centerY=endHeight-40+t*42;
        if(x>=startX&&x<=startX+49&&sq((z-startZ)/3.0)+sq((y-centerY)/3.0)<1)return true;
        return sq(x-startX)+sq(z-startZ)<9&&y>=endHeight-60&&y<=endHeight-38;
    }
    private GeyserTerrain(){}
}
