package org.Azkerash.riftfall.crystals;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalGapConnectionsTest {
    @Test void oneClearCellConnectsMatureEndpointsInAllSixDirections(){for(var d:Direction.values()){var air=new CrystalSurfaceConnections.Offset(d.getStepX(),d.getStepY(),d.getStepZ());var mature=new CrystalSurfaceConnections.Offset(d.getStepX()*2,d.getStepY()*2,d.getStepZ()*2);assertEquals(CrystalSurfaceConnections.bit(d),CrystalGapConnections.directions(p->p.equals(air),p->p.equals(mature)));}}
    @Test void obstructionsOrImmatureEndpointsPreventABridge(){assertEquals(0,CrystalGapConnections.directions(p->false,p->true));assertEquals(0,CrystalGapConnections.directions(p->true,p->false));}
    @Test void twoEmptyCellsAreTooFar(){assertEquals(0,CrystalGapConnections.directions(p->true,p->Math.abs(p.x())+Math.abs(p.y())+Math.abs(p.z())==3));}
}
