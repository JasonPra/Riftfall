package org.Azkerash.riftfall.crystals;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalSurfaceColorsTest {
    @Test void convexCornersMixOnEverySurfaceAndTangent(){
        for(var face:Direction.values())for(int edge=0;edge<4;edge++){
            var side=CrystalSurfaceConnections.tangent(face,edge);
            var target=new CrystalSurfaceConnections.Offset(side.getStepX()-face.getStepX(),side.getStepY()-face.getStepY(),side.getStepZ()-face.getStepZ());
            var colors=CrystalSurfaceColors.forFace(face,p->p.equals(target)?new CrystalSurfaceColors.Contact(CrystalTier.RED,CrystalSurfaceConnections.bit(side)):null);
            assertEquals(CrystalTier.RED,colors[side.ordinal()],face+" "+side);
            var invalid=CrystalSurfaceColors.forFace(face,p->p.equals(target)?new CrystalSurfaceColors.Contact(CrystalTier.RED,CrystalSurfaceConnections.bit(side.getOpposite())):null);
            assertNull(invalid[side.ordinal()],"An unrelated diagonal must not leak color through stone");
        }
    }
    @Test void directContactTakesPrecedenceOverDiagonal(){
        var colors=CrystalSurfaceColors.forFace(Direction.UP,p->{
            if(p.equals(new CrystalSurfaceConnections.Offset(1,0,0)))return new CrystalSurfaceColors.Contact(CrystalTier.WHITE,CrystalSurfaceConnections.bit(Direction.UP));
            if(p.equals(new CrystalSurfaceConnections.Offset(1,-1,0)))return new CrystalSurfaceColors.Contact(CrystalTier.RED,CrystalSurfaceConnections.bit(Direction.EAST));
            return null;
        });
        assertEquals(CrystalTier.WHITE,colors[Direction.EAST.ordinal()]);
    }
}
