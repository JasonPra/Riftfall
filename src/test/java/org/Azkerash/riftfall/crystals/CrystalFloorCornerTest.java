package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalFloorCornerTest {
    @Test void convexFloorToWallJunctionsExtendOnlyTheMatchingEdgeInAllFourDirections(){
        for(var side:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST))for(int stage=1;stage<=8;stage++){
            var target=new CrystalSurfaceConnections.Offset(side.getStepX(),-1,side.getStepZ());
            int growth=stage;
            var edges=CrystalFloorCorner.extensions(Direction.UP,p->p.equals(target)?new CrystalFloorCorner.Wall(CrystalSurfaceConnections.bit(side),growth,side):null);
            assertEquals(Set.of(side),edges.keySet());assertEquals(Math.min(1.2,stage*.6)/16,edges.get(side),1e-12);
        }
    }
    @Test void nonOwningWallsCeilingsAndMissingOrUnrelatedCrystalsDoNotExtend(){
        for(var face:Direction.values())if(face==Direction.DOWN||face.getAxis()==Direction.Axis.X)assertTrue(CrystalFloorCorner.extensions(face,p->{fail("Non-owning walls and ceilings must not query for extension");return null;}).isEmpty());
        assertTrue(CrystalFloorCorner.extensions(Direction.UP,p->null).isEmpty());
        assertTrue(CrystalFloorCorner.extensions(Direction.UP,p->new CrystalFloorCorner.Wall(CrystalSurfaceConnections.bit(Direction.UP),8,Direction.UP)).isEmpty());
    }
    @Test void verticalWallCornersHaveExactlyOneOwnerInAllFourOrientations(){
        for(var face:List.of(Direction.NORTH,Direction.SOUTH))for(var side:List.of(Direction.EAST,Direction.WEST))for(int stage=1;stage<=8;stage++){
            var target=new CrystalSurfaceConnections.Offset(side.getStepX()-face.getStepX(),0,side.getStepZ()-face.getStepZ());int layers=stage;
            var edges=CrystalFloorCorner.extensions(face,p->p.equals(target)?new CrystalFloorCorner.Wall(CrystalSurfaceConnections.bit(side),layers,side):null);
            assertEquals(Set.of(side),edges.keySet());assertEquals(Math.min(1.2,stage*.6)/16,edges.get(side),1e-12);
            assertTrue(CrystalFloorCorner.extensions(side,p->{fail("Opposite wall must remain unchanged");return null;}).isEmpty());
        }
    }
    @Test void flatWallNeighborsDoNotTriggerCornerExtensions(){
        assertTrue(CrystalFloorCorner.extensions(Direction.NORTH,p->p.equals(new CrystalSurfaceConnections.Offset(1,0,0))?new CrystalFloorCorner.Wall(CrystalSurfaceConnections.bit(Direction.EAST),3,Direction.EAST):null).isEmpty());
    }
    @Test void secondaryWallGrowthUsesItsActualBedThickness(){
        var edges=CrystalFloorCorner.extensions(Direction.UP,p->p.equals(new CrystalSurfaceConnections.Offset(1,-1,0))?new CrystalFloorCorner.Wall(CrystalSurfaceConnections.bit(Direction.UP)|CrystalSurfaceConnections.bit(Direction.EAST),2,Direction.UP):null);
        assertEquals(.6/16,edges.get(Direction.EAST),1e-12);
    }
}
