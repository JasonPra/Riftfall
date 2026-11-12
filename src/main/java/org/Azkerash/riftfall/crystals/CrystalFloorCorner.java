package org.Azkerash.riftfall.crystals;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.Direction;

/** Small floor-bed extension only at a convex junction with an actual wall crystal. */
public final class CrystalFloorCorner {
    public record Wall(int surfaces,int layers,Direction primary){
        public Wall {if(layers<1||layers>8)throw new IllegalArgumentException("Invalid crystal growth layer");Objects.requireNonNull(primary);}
    }
    public static Map<Direction,Double> extensions(Direction face,Function<CrystalSurfaceConnections.Offset,Wall> lookup){
        if(face!=Direction.UP)return Map.of();
        var result=new EnumMap<Direction,Double>(Direction.class);
        for(int edge=0;edge<4;edge++){
            var side=CrystalSurfaceConnections.tangent(Direction.UP,edge);
            var wall=lookup.apply(new CrystalSurfaceConnections.Offset(side.getStepX(),-1,side.getStepZ()));
            if(wall==null||!CrystalSurfaceConnections.includes(wall.surfaces(),side))continue;
            int growth=wall.primary()==side?wall.layers():(wall.layers()+1)/2;
            result.put(side,Math.min(1.2,growth*.6)/16.0);
        }
        return Map.copyOf(result);
    }
    private CrystalFloorCorner(){}
}
