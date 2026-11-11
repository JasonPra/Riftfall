package org.Azkerash.riftfall.crystals;
import java.util.*;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalSurfaceConnectionsTest {
    @Test void everySurfaceJoinsItsNeighborsInAllFourDirections(){
        for(var face:Direction.values())for(int side=0;side<4;side++){
            var direction=CrystalSurfaceConnections.tangent(face,side);var offset=new CrystalSurfaceConnections.Offset(direction.getStepX(),direction.getStepY(),direction.getStepZ());
            assertEquals(1<<side,CrystalSurfaceConnections.joins(face,p->p.equals(offset)?CrystalSurfaceConnections.bit(face):0));
            // The common surface can be secondary for either crystal: original placement is irrelevant.
            int secondary=CrystalSurfaceConnections.bit(face)|CrystalSurfaceConnections.bit(direction);
            assertEquals(1<<side,CrystalSurfaceConnections.joins(face,p->p.equals(offset)?secondary:0));
        }
    }
    @Test void convexCornersWrapAllTwentyFourOrientationPairs(){
        for(var face:Direction.values())for(int side=0;side<4;side++){
            var edge=CrystalSurfaceConnections.tangent(face,side);var around=new CrystalSurfaceConnections.Offset(edge.getStepX()-face.getStepX(),edge.getStepY()-face.getStepY(),edge.getStepZ()-face.getStepZ());
            assertEquals(1<<side,CrystalSurfaceConnections.joins(face,p->p.equals(around)?CrystalSurfaceConnections.bit(edge):0));
        }
    }
    @Test void rootsWithinOneClusterMeetAtEverySolidInsideCorner(){
        for(var face:Direction.values())for(int side=0;side<4;side++){
            var edge=CrystalSurfaceConnections.tangent(face,side);int surfaces=CrystalSurfaceConnections.bit(face)|CrystalSurfaceConnections.bit(edge.getOpposite());
            assertEquals(1<<side,CrystalSurfaceConnections.joins(face,surfaces,p->0));
            var other=edge.getOpposite();int reciprocal=CrystalSurfaceConnections.joins(other,surfaces,p->0);
            assertEquals(1,Integer.bitCount(reciprocal),"Both surfaces need a root along their common edge even without another crystal");
        }
    }
    @Test void threeSurfaceCornersRetainIndependentJoinsWithoutAnEndCap(){
        for(var wall:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            int faces=CrystalSurfaceConnections.bit(Direction.UP)|CrystalSurfaceConnections.bit(wall)|CrystalSurfaceConnections.bit(wall.getClockWise());
            assertEquals(3,Integer.bitCount(faces));assertTrue(CrystalSurfaceConnections.includes(faces,Direction.UP));
            assertEquals(15,CrystalSurfaceConnections.joins(wall,p->faces));
        }
        for(var face:Direction.values())assertEquals(0,CrystalSurfaceConnections.joins(face,p->0));
    }
}
