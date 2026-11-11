package org.Azkerash.riftfall.client;
import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalBorderMaskTest {
    @Test void sharedBordersAreClippedAndOnlyTheExposedStripSurvives(){
        for(var side:Direction.values())for(var growth:Direction.values())if(side.getAxis()!=growth.getAxis()){
            var original=CrystalBorderMask.make(side,growth,.12,null);var source=new BakedQuad(original.getVertices(),42,side,null,true,true);var mask=CrystalBorderMask.make(side,growth,.08,null);
            var output=CrystalQuadUnion.clean(List.of(source,mask),q->q.getTintIndex()==CrystalBorderMask.ID?Integer.MAX_VALUE:0).stream().filter(q->q.getTintIndex()!=CrystalBorderMask.ID).toList();
            double area=0;for(var q:output){var d=q.getVertices();double[] a=new double[3],b=new double[3];for(int k=0;k<3;k++){a[k]=Float.intBitsToFloat(d[8+k])-Float.intBitsToFloat(d[k]);b[k]=Float.intBitsToFloat(d[24+k])-Float.intBitsToFloat(d[k]);}double x=a[1]*b[2]-a[2]*b[1],y=a[2]*b[0]-a[0]*b[2],z=a[0]*b[1]-a[1]*b[0];area+=Math.sqrt(x*x+y*y+z*z);}assertEquals(.04,area,1e-6,side+" "+growth);
        }
    }
}
