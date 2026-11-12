package org.Azkerash.riftfall.crystals;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.Direction;

/** Small bed-edge extensions at connected floor/wall and vertical wall/wall corners. */
public final class CrystalFloorCorner {
    public record Wall(int surfaces,int layers,Direction primary){
        public Wall {if(layers<1||layers>8)throw new IllegalArgumentException("Invalid crystal growth layer");Objects.requireNonNull(primary);}
    }
    public static Map<Direction,Double> extensions(Direction face,Function<CrystalSurfaceConnections.Offset,Wall> lookup){
        // Floors own floor/wall joins. North/south walls own vertical joins with east/west walls.
        // The opposite participant never also extends into the corner.
        if(face!=Direction.UP&&face.getAxis()!=Direction.Axis.Z)return Map.of();
        var result=new EnumMap<Direction,Double>(Direction.class);
        for(int edge=0;edge<4;edge++){
            var side=CrystalSurfaceConnections.tangent(face,edge);
            if(face!=Direction.UP&&side.getAxis()!=Direction.Axis.X)continue;
            var wall=lookup.apply(new CrystalSurfaceConnections.Offset(side.getStepX()-face.getStepX(),side.getStepY()-face.getStepY(),side.getStepZ()-face.getStepZ()));
            if(wall==null||!CrystalSurfaceConnections.includes(wall.surfaces(),side))continue;
            int growth=wall.primary()==side?wall.layers():(wall.layers()+1)/2;
            result.put(side,Math.min(1.2,growth*.6)/16.0);
        }
        return Map.copyOf(result);
    }
    private CrystalFloorCorner(){}
}
