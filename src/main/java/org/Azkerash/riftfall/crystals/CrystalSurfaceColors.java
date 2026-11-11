package org.Azkerash.riftfall.crystals;

import java.util.function.Function;
import net.minecraft.core.Direction;

/** Match color contacts to the same direct and convex-corner topology used by the mesh. */
public final class CrystalSurfaceColors {
    public record Contact(CrystalTier tier,int surfaces){}
    public static CrystalTier[] forFace(Direction face,Function<CrystalSurfaceConnections.Offset,Contact> lookup){
        var colors=new CrystalTier[6];
        for(var side:Direction.values()){
            var direct=lookup.apply(new CrystalSurfaceConnections.Offset(side.getStepX(),side.getStepY(),side.getStepZ()));
            if(direct!=null)colors[side.ordinal()]=direct.tier();
        }
        for(int edge=0;edge<4;edge++){
            var side=CrystalSurfaceConnections.tangent(face,edge);
            if(colors[side.ordinal()]!=null)continue;
            var around=lookup.apply(new CrystalSurfaceConnections.Offset(side.getStepX()-face.getStepX(),side.getStepY()-face.getStepY(),side.getStepZ()-face.getStepZ()));
            if(around!=null&&CrystalSurfaceConnections.includes(around.surfaces(),side))colors[side.ordinal()]=around.tier();
        }
        return colors;
    }
    private CrystalSurfaceColors(){}
}
