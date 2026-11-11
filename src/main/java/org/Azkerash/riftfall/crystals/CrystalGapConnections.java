package org.Azkerash.riftfall.crystals;
import java.util.function.Function;
import net.minecraft.core.Direction;
/** Mature endpoints can join through exactly one clear air cell, in any of six directions. */
public final class CrystalGapConnections {
    public static int directions(Function<CrystalSurfaceConnections.Offset,Boolean> air,Function<CrystalSurfaceConnections.Offset,Boolean> mature){
        int mask=0;for(var face:Direction.values()){
            var gap=new CrystalSurfaceConnections.Offset(face.getStepX(),face.getStepY(),face.getStepZ());var target=new CrystalSurfaceConnections.Offset(face.getStepX()*2,face.getStepY()*2,face.getStepZ()*2);
            if(air.apply(gap)&&mature.apply(target))mask|=CrystalSurfaceConnections.bit(face);
        }return mask;
    }
    private CrystalGapConnections(){}
}
