package org.Azkerash.riftfall.worldmana.geysers;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;

/** Full-height field queries, plus a stricter cave-surface predicate for future crystal generation. */
public final class GeyserQueries {
    public static Optional<GeyserRegion> at(ServerLevel level,BlockPos pos){return GeyserRegions.get(level).discover(pos.getX()>>4,pos.getZ()>>4);}
    public static boolean crystalSurface(ServerLevel level,BlockPos pos){
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)return false;
        var field=at(level,pos);if(field.isEmpty()||!field.orElseThrow().crystalZone(pos.getX()>>4,pos.getZ()>>4))return false;
        if(!chunk.getBlockState(pos).isAir()||!chunk.getFluidState(pos).isEmpty())return false;
        if(pos.getY()<=level.getMinBuildHeight()||pos.getY()>=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,pos.getX()&15,pos.getZ()&15)-8)return false;
        for(var direction:Direction.values()){
            var host=pos.relative(direction);var neighbor=level.getChunkSource().getChunkNow(host.getX()>>4,host.getZ()>>4);if(neighbor==null)continue;
            var stone=neighbor.getBlockState(host);
            if((stone.is(BlockTags.BASE_STONE_OVERWORLD)||stone.is(BlockTags.BASE_STONE_NETHER))&&stone.isFaceSturdy(level,host,direction.getOpposite()))return true;
        }return false;
    }
    private GeyserQueries(){}
}
