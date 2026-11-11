package org.Azkerash.riftfall.crystals;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalSurfaceConnectionsTest {
    @Test void connectionsWrapAllPerpendicularSurfaceOrientations(){
        for(var face:Direction.values())for(var seam:Direction.values())if(face.getAxis()!=seam.getAxis()){
            assertTrue(CrystalSurfaceConnections.edge(face,seam,face,false));
            assertFalse(CrystalSurfaceConnections.edge(face,seam,face.getOpposite(),true));
            assertTrue(CrystalSurfaceConnections.edge(face,seam,seam.getOpposite(),true));
            assertFalse(CrystalSurfaceConnections.edge(face,seam,seam.getOpposite(),false),"An immature end cannot bridge an entire voxel");
            assertTrue(CrystalSurfaceConnections.tip(seam.getOpposite(),true,face));
            assertFalse(CrystalSurfaceConnections.tip(seam.getOpposite(),false,face));
            for(var cross:Direction.values())if(cross.getAxis()!=face.getAxis()&&cross.getAxis()!=seam.getAxis())assertTrue(CrystalSurfaceConnections.edge(face,seam,cross,false));
        }
    }
    @Test void exposedEndsDoNotCreateUnsupportedParallelStacks(){
        for(var face:Direction.values()){
            assertFalse(CrystalSurfaceConnections.tip(face,true,face));assertFalse(CrystalSurfaceConnections.tip(face,true,face.getOpposite()));
            assertThrows(IllegalArgumentException.class,()->CrystalSurfaceConnections.edge(face,face,face,true));
        }
    }
}
