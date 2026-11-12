package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalCoplanarFacesTest {
    private static BakedQuad quad(float x0,float z0,float x1,float z1,double tilt,float y,int tint,boolean reverse){
        float[][] corners={{x0,z0},{x0,z1},{x1,z1},{x1,z0}};int[] data=new int[32];double angle=Math.toRadians(tilt),c=Math.cos(angle),s=Math.sin(angle);
        for(int i=0;i<4;i++){
            int j=reverse?3-i:i;double z=corners[j][1];data[i*8]=Float.floatToRawIntBits(corners[j][0]);data[i*8+1]=Float.floatToRawIntBits((float)(.5+(y-.5)*c-(z-.5)*s));data[i*8+2]=Float.floatToRawIntBits((float)(.5+(y-.5)*s+(z-.5)*c));
            data[i*8+3]=-1;data[i*8+4]=Float.floatToRawIntBits(corners[j][0]);data[i*8+5]=Float.floatToRawIntBits(corners[j][1]);data[i*8+6]=1234;data[i*8+7]=5678;
        }
        return new BakedQuad(data,tint,reverse?Direction.DOWN:Direction.UP,null,true,true);
    }
    private static List<List<BakedQuad>> model(BakedQuad... quads){var result=new ArrayList<List<BakedQuad>>();for(int i=0;i<6;i++)result.add(List.of());result.add(List.of(quads));return result;}
    private static double localZ(int[] data,int i,double tilt){double angle=Math.toRadians(tilt);return .5-(Float.intBitsToFloat(data[i*8+1])-.5)*Math.sin(angle)+(Float.intBitsToFloat(data[i*8+2])-.5)*Math.cos(angle);}
    private static double area(List<BakedQuad> quads,double tilt){double total=0;for(var quad:quads){var data=quad.getVertices();for(int i=1;i<3;i++){double ax=Float.intBitsToFloat(data[i*8])-Float.intBitsToFloat(data[0]),az=localZ(data,i,tilt)-localZ(data,0,tilt),bx=Float.intBitsToFloat(data[(i+1)*8])-Float.intBitsToFloat(data[0]),bz=localZ(data,i+1,tilt)-localZ(data,0,tilt);total+=Math.abs(ax*bz-az*bx)*.5;}}return total;}
    private static boolean covers(BakedQuad quad,double x,double z,double tilt){
        var data=quad.getVertices();
        for(int i=0;i<4;i++){int j=(i+1)%4;double ax=Float.intBitsToFloat(data[i*8]),az=localZ(data,i,tilt),bx=Float.intBitsToFloat(data[j*8]),bz=localZ(data,j,tilt);if((bx-ax)*(z-az)-(bz-az)*(x-ax)>1e-7)return false;}
        return true;
    }
    @Test void duplicateCoverageIsRemovedWithoutChangingShapeUvsOrOpacityEvenOnTiltedFaces(){
        for(double tilt:new double[]{0,22.5,-22.5,90}){
            var first=quad(0,0,.75f,.75f,tilt,.2f,17,false);var second=quad(.25f,.25f,1,1,tilt,.2f,18,false);
            var a=model(first);var b=model(second);CrystalCoplanarFaces.clean(List.of(a,b));
            assertSame(first,a.get(6).getFirst());var output=new ArrayList<>(a.get(6));output.addAll(b.get(6));assertEquals(.875,area(output,tilt),1e-6);
            for(var q:output){var data=q.getVertices();for(int i=0;i<4;i++){
                assertEquals(Float.intBitsToFloat(data[i*8]),Float.intBitsToFloat(data[i*8+4]),1e-6);assertEquals(localZ(data,i,tilt),Float.intBitsToFloat(data[i*8+5]),1e-6);
                assertEquals(-1,data[i*8+3]);assertEquals(1234,data[i*8+6]);assertEquals(5678,data[i*8+7]);
                double angle=Math.toRadians(tilt),localY=.5+(Float.intBitsToFloat(data[i*8+1])-.5)*Math.cos(angle)+(Float.intBitsToFloat(data[i*8+2])-.5)*Math.sin(angle);assertEquals(.2,localY,1e-6);
            }}
            for(int ix=0;ix<20;ix++)for(int iz=0;iz<20;iz++){
                double x=.0123+ix*.0467,z=.0234+iz*.0479;int count=0;for(var q:output)if(covers(q,x,z,tilt))count++;
                boolean expected=x<=.75&&z<=.75||x>=.25&&z>=.25;assertEquals(expected?1:0,count,"Each original surface point has one stable owner");
            }
        }
    }
    @Test void distinctPlanesOppositeFacesAndSeedBedsAreUntouched(){
        var first=quad(0,0,1,1,22.5,.2f,17,false);var raised=quad(0,0,1,1,22.5,.3f,18,false);var crossing=quad(0,0,1,1,-22.5,.2f,19,false);
        var back=quad(0,0,1,1,22.5,.2f,20,true);var bed=quad(0,0,1,1,22.5,.2f,65537,false);
        var model=model(first,raised,crossing,back,bed);CrystalCoplanarFaces.clean(List.of(model));assertEquals(List.of(first,raised,crossing,back,bed),model.get(6));
    }
    @Test void exactPrismDuplicatesHaveOneOwnerAndRepeatBuildsAreIdentical(){
        var first=quad(0,0,1,1,22.5,.2f,17,false);var second=quad(0,0,1,1,22.5,.2f,18,false);
        for(int repeat=0;repeat<3;repeat++){var a=model(first);var b=model(second);CrystalCoplanarFaces.clean(List.of(a,b));assertEquals(List.of(first),a.get(6));assertTrue(b.get(6).isEmpty());}
    }
}
