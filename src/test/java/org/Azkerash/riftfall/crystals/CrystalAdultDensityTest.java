package org.Azkerash.riftfall.crystals;
import java.util.*;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalAdultDensityTest {
    @Test void densityIncreasesFromPairToSquareToSurroundedOnEverySurface(){
        for(var face:Direction.values()){
            var a=CrystalSurfaceConnections.tangent(face,0);var b=CrystalSurfaceConnections.tangent(face,1);
            var first=new CrystalSurfaceConnections.Offset(a.getStepX(),a.getStepY(),a.getStepZ());var second=new CrystalSurfaceConnections.Offset(b.getStepX(),b.getStepY(),b.getStepZ());
            var diagonal=new CrystalSurfaceConnections.Offset(a.getStepX()+b.getStepX(),a.getStepY()+b.getStepY(),a.getStepZ()+b.getStepZ());
            var isolated=CrystalAdultDensity.profile(face,p->false);var pair=CrystalAdultDensity.profile(face,p->p.equals(first));
            var square=CrystalAdultDensity.profile(face,Set.of(first,second,diagonal)::contains);var surrounded=CrystalAdultDensity.profile(face,p->true);
            assertEquals(0,isolated.neighbors());assertEquals(1,isolated.xScale());assertEquals(1,isolated.zScale());
            assertEquals(1,pair.neighbors());assertEquals(0,pair.corners());assertEquals(2,square.neighbors());assertEquals(1,square.corners());
            assertTrue(pair.xScale()>1&&pair.zScale()>1);assertTrue(square.xScale()>pair.xScale()&&square.zScale()>pair.zScale());
            assertEquals(4,surrounded.neighbors());assertEquals(4,surrounded.corners());assertTrue(surrounded.xScale()>square.xScale());assertTrue(surrounded.zScale()>square.zScale());
        }
    }
    @Test void aMissingDiagonalDoesNotGrantTheCompleteSquareBonus(){
        var face=Direction.UP;var a=new CrystalSurfaceConnections.Offset(0,0,-1);var b=new CrystalSurfaceConnections.Offset(1,0,0);
        assertEquals(0,CrystalAdultDensity.profile(face,Set.of(a,b)::contains).corners());
    }
    @Test void fittedPrismsStayInsideTheCellWithoutCollapsing(){
        for(double min:new double[]{0,.1,.4,.8,.95})for(double width:new double[]{.01,.1,.3})if(min+width<=1)for(double scale:new double[]{1,1.26,1.5,2.05})for(double shift:new double[]{-.035,0,.035}){
            var fit=CrystalAdultDensity.fit(min,min+width,scale,shift);assertTrue(fit[0]>=0);assertTrue(fit[1]<=1+1e-9);assertTrue(fit[1]>fit[0]);assertEquals(Math.min(1,width*scale),fit[1]-fit[0],1e-9);
        }
    }
}
