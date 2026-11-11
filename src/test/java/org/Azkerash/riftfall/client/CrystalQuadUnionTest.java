package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalQuadUnionTest {
    private BakedQuad quad(double x0,double z0,double x1,double z1,double y,int tier){
        int[] data=new int[32];double[][] positions={{x0,y,z0},{x0,y,z1},{x1,y,z1},{x1,y,z0}};float[][] uv={{0,0},{0,1},{1,1},{1,0}};
        for(int i=0;i<4;i++){for(int a=0;a<3;a++)data[i*8+a]=Float.floatToRawIntBits((float)positions[i][a]);data[i*8+3]=-1;data[i*8+4]=Float.floatToRawIntBits(uv[i][0]);data[i*8+5]=Float.floatToRawIntBits(uv[i][1]);}
        return new BakedQuad(data,tier,Direction.UP,null,false,false);
    }
    private double area(BakedQuad quad){var d=quad.getVertices();double total=0;for(int[] triangle:new int[][]{{0,1,2},{0,2,3}}){double[] a=new double[3],b=new double[3];for(int axis=0;axis<3;axis++){a[axis]=Float.intBitsToFloat(d[triangle[1]*8+axis])-Float.intBitsToFloat(d[triangle[0]*8+axis]);b[axis]=Float.intBitsToFloat(d[triangle[2]*8+axis])-Float.intBitsToFloat(d[triangle[0]*8+axis]);}double x=a[1]*b[2]-a[2]*b[1],y=a[2]*b[0]-a[0]*b[2],z=a[0]*b[1]-a[1]*b[0];total+=Math.sqrt(x*x+y*y+z*z)*.5;}return total;}
    private double area(List<BakedQuad> list){return list.stream().mapToDouble(this::area).sum();}
    @Test void highestTierOwnsCoincidentFacesRegardlessOfInputOrder(){
        var low=quad(0,0,1,1,.1,0);var high=quad(0,0,1,1,.1,7);
        for(var input:List.of(List.of(low,high),List.of(high,low))){var result=CrystalQuadUnion.clean(input,BakedQuad::getTintIndex);assertEquals(1,result.size());assertSame(high,result.getFirst());}
    }
    @Test void sharedCornerFromAnotherBlockClipsOnlyTheDuplicateStrip(){
        var neighbor=quad(.9375,.2,1.0625,.8,.1,7);var mask=CrystalNeighborFaces.mask(neighbor,-1,0,0);assertNotNull(mask);
        var own=quad(0,.2,.125,.8,.1,0);
        var result=CrystalQuadUnion.clean(List.of(own,mask),q->q.getTintIndex()==CrystalNeighborFaces.MASK_ID?Integer.MAX_VALUE:0);
        var rendered=result.stream().filter(q->q.getTintIndex()!=CrystalNeighborFaces.MASK_ID).toList();
        assertEquals(.0625*.6,area(rendered),1e-6);assertEquals(.125*.6,area(List.of(neighbor)),1e-6);
        for(var q:rendered)for(int v=0;v<4;v++)assertTrue(Float.intBitsToFloat(q.getVertices()[v*8])>=.0625-1e-6);
        assertNull(CrystalNeighborFaces.mask(neighbor,-3,0,0));
    }
    @Test void sharedPiecesDoNotFightWithTheStoneSupportFace(){
        var own=quad(.9,.2,1.1,.8,0,7);var solid=CrystalNeighborFaces.solidFace(Direction.UP,0,-1,0);assertNotNull(solid);
        var result=CrystalQuadUnion.clean(List.of(own,solid),q->q.getTintIndex()==CrystalNeighborFaces.MASK_ID?Integer.MAX_VALUE:0);
        assertEquals(.1*.6,area(result.stream().filter(q->q.getTintIndex()!=CrystalNeighborFaces.MASK_ID).toList()),1e-6);
    }
    @Test void trimmedFacesKeepFrontFacingTriangleWindingOnAllSixAttachments(){
        for(var face:Direction.values()){
            var input=new ArrayList<BakedQuad>();for(var q:List.of(quad(0,0,1,1,.1,0),quad(.2,.1,.5,.6,.1,1),quad(.4,.3,.8,.8,.1,2),quad(.1,.7,.7,.9,.1,3)))input.add(CrystalSurfaceRotation.rotate(q,face));
            for(var q:CrystalQuadUnion.clean(input,BakedQuad::getTintIndex)){
                var d=q.getVertices();for(int[] t:new int[][]{{0,1,2},{0,2,3}}){
                    double[] a=new double[3],b=new double[3];for(int k=0;k<3;k++){a[k]=Float.intBitsToFloat(d[t[1]*8+k])-Float.intBitsToFloat(d[t[0]*8+k]);b[k]=Float.intBitsToFloat(d[t[2]*8+k])-Float.intBitsToFloat(d[t[0]*8+k]);}
                    double dot=(a[1]*b[2]-a[2]*b[1])*face.getStepX()+(a[2]*b[0]-a[0]*b[2])*face.getStepY()+(a[0]*b[1]-a[1]*b[0])*face.getStepZ();assertTrue(dot>=-1e-8,"Clipping must never turn an outer face inward");
                }
            }
        }
    }
    @Test void equalTierFacesHaveDeterministicOwnership(){
        var first=quad(0,0,1,1,.1,3);var second=quad(0,0,1,1,.1,3);
        for(int i=0;i<10;i++)assertSame(first,CrystalQuadUnion.clean(List.of(first,second),BakedQuad::getTintIndex).getFirst());
    }
    @Test void partialOverlapIsTrimmedWithoutHolesOrUvStretching(){
        var high=quad(0,0,1,1,.1,7);var low=quad(.5,0,1.5,1,.1,0);var result=CrystalQuadUnion.clean(List.of(low,high),BakedQuad::getTintIndex);
        assertEquals(2,result.size());assertEquals(1.5,area(result),1e-7);assertSame(high,result.getFirst());var clipped=result.get(1);var data=clipped.getVertices();
        for(int i=0;i<4;i++){double x=Float.intBitsToFloat(data[i*8]);assertTrue(x>=1&&x<=1.5);assertEquals(x-.5,Float.intBitsToFloat(data[i*8+4]),1e-7);}
    }
    @Test void rootsCrossingAtACornerHaveExactlyTheirUnionArea(){
        var a=quad(0,0,1,.5,.075,2);var b=quad(.5,0,1,1,.075,2);var c=quad(.2,.2,.8,.8,.075,2);
        var result=CrystalQuadUnion.clean(List.of(a,b,c),BakedQuad::getTintIndex);assertEquals(.84,area(result),1e-6);
    }
    @Test void DifferentPlanesAndTiltedFacesKeepTheirGeometry(){
        var a=quad(0,0,1,1,.1,1);var b=quad(0,0,1,1,.2,7);assertEquals(2,CrystalQuadUnion.clean(List.of(a,b),BakedQuad::getTintIndex).size());
        var tilted=quad(0,0,1,1,.1,1);tilted.getVertices()[1]=Float.floatToRawIntBits(.3F);var result=CrystalQuadUnion.clean(List.of(a,tilted),BakedQuad::getTintIndex);assertEquals(2,result.size());assertTrue(result.contains(tilted));
    }
    @Test void clippingPreservesCoverageOnAllSixSurfaceDirections(){
        for(var direction:Direction.values()){
            var input=new ArrayList<BakedQuad>();for(var source:List.of(quad(0,0,1,1,.1,7),quad(.5,0,1.5,1,.1,0))){
                var data=source.getVertices().clone();for(int vertex=0;vertex<4;vertex++){
                    int offset=vertex*8;if(direction.getAxis()==Direction.Axis.X){int swap=data[offset];data[offset]=data[offset+1];data[offset+1]=swap;}
                    if(direction.getAxis()==Direction.Axis.Z){int swap=data[offset+1];data[offset+1]=data[offset+2];data[offset+2]=swap;}
                }input.add(new BakedQuad(data,source.getTintIndex(),direction,null,false,false));
            }
            assertEquals(1.5,area(CrystalQuadUnion.clean(input,BakedQuad::getTintIndex)),1e-6,direction.toString());
        }
    }
    @Test void opposingFacesAreNotRemovedAsDuplicates(){
        var up=quad(0,0,1,1,.1,3);var down=new BakedQuad(up.getVertices().clone(),3,Direction.DOWN,null,false,false);
        assertEquals(2,CrystalQuadUnion.clean(List.of(up,down),BakedQuad::getTintIndex).size());
    }

    @Test void coplanarRotatedStemFacesAreTrimmedToo(){
        var base=quad(0,0,1,1,.1,7);var rotated=quad(0,0,1,1,.1,0);double angle=Math.PI/4;
        for(int i=0;i<4;i++){double x=Float.intBitsToFloat(rotated.getVertices()[i*8])-.5,z=Float.intBitsToFloat(rotated.getVertices()[i*8+2])-.5;rotated.getVertices()[i*8]=Float.floatToRawIntBits((float)(.5+x*Math.cos(angle)-z*Math.sin(angle)));rotated.getVertices()[i*8+2]=Float.floatToRawIntBits((float)(.5+x*Math.sin(angle)+z*Math.cos(angle)));}
        assertEquals(4-2*Math.sqrt(2),area(CrystalQuadUnion.clean(List.of(rotated,base),BakedQuad::getTintIndex)),1e-6);
    }

}
