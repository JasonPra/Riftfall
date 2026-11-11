package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalBedModelTest {
    private static BakedQuad top(float x0,float z0,float x1,float z1,float y,int id,Direction direction){
        float[][] points={{x0,z0},{x0,z1},{x1,z1},{x1,z0}};int[] data=new int[32];
        for(int i=0;i<4;i++){
            int j=direction==Direction.UP?i:3-i;
            data[i*8]=Float.floatToRawIntBits(points[j][0]);data[i*8+1]=Float.floatToRawIntBits(y);data[i*8+2]=Float.floatToRawIntBits(points[j][1]);
            data[i*8+3]=-1;data[i*8+4]=data[i*8];data[i*8+5]=data[i*8+2];data[i*8+6]=1234;data[i*8+7]=5678;
        }
        return new BakedQuad(data,id,direction,null,true,true);
    }
    private static CrystalBedModel model(BakedQuad... quads){
        BakedModel raw=new BakedModelWrapper<BakedModel>(null){@Override public List<BakedQuad> getQuads(BlockState s,Direction side,RandomSource r,ModelData d,RenderType type){return side==null?List.of(quads):List.of();}};
        return new CrystalBedModel(raw,null,1);
    }
    private static List<BakedQuad> quads(CrystalBedModel model){return model.getQuads(null,null,RandomSource.create(1),ModelData.EMPTY,null);}
    private static double[] bounds(BakedQuad quad){
        double[] b={99,99,-99,-99};var d=quad.getVertices();
        for(int i=0;i<4;i++){double x=Float.intBitsToFloat(d[i*8]),z=Float.intBitsToFloat(d[i*8+2]);b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],z);b[2]=Math.max(b[2],x);b[3]=Math.max(b[3],z);}return b;
    }
    @Test void overlappingBedsHaveOneOwnerWithoutLosingCoverageOrChangingUvs(){
        var a=model(top(0,0,.75f,.75f,.1f,65537,Direction.UP));
        var b=model(top(.25f,.25f,1,1,.1f,65538,Direction.UP));
        CrystalBedModel.clean(List.of(a,b));
        var output=new ArrayList<>(quads(a));output.addAll(quads(b));double area=0;
        for(int i=0;i<output.size();i++){
            var q=output.get(i);var box=bounds(q);area+=(box[2]-box[0])*(box[3]-box[1]);
            for(int j=0;j<i;j++){var other=bounds(output.get(j));assertTrue(Math.min(box[2],other[2])-Math.max(box[0],other[0])<=1e-6||Math.min(box[3],other[3])-Math.max(box[1],other[1])<=1e-6);}
            var data=q.getVertices();
            for(int v=0;v<4;v++){
                assertEquals(data[v*8],data[v*8+4]);assertEquals(data[v*8+2],data[v*8+5]);
                assertEquals(Float.floatToRawIntBits(.1f),data[v*8+1]);assertEquals(-1,data[v*8+3]);assertEquals(1234,data[v*8+6]);assertEquals(5678,data[v*8+7]);
            }
            double ax=Float.intBitsToFloat(data[8])-Float.intBitsToFloat(data[0]),az=Float.intBitsToFloat(data[10])-Float.intBitsToFloat(data[2]);
            double bx=Float.intBitsToFloat(data[24])-Float.intBitsToFloat(data[0]),bz=Float.intBitsToFloat(data[26])-Float.intBitsToFloat(data[2]);
            assertTrue(az*bx-ax*bz>0,"Fragments preserve outward winding");
        }
        assertEquals(.875,area,1e-8);
    }
    @Test void cleanupHandlesFloorsWallsAndCeilings(){
        for(var direction:Direction.values()){
            int axis=direction.getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;
            var flat=top(0,0,1,1,.1f,65537,Direction.UP);var data=flat.getVertices().clone();
            for(int i=0;i<4;i++){
                data[i*8+axis]=Float.floatToRawIntBits(.25f);
                data[i*8+u]=flat.getVertices()[i*8];data[i*8+v]=flat.getVertices()[i*8+2];
            }
            var quad=new BakedQuad(data,65537,direction,null,true,true);
            var a=model(quad);var b=model(quad);CrystalBedModel.clean(List.of(a,b));
            assertEquals(1,quads(a).size());assertTrue(quads(b).isEmpty(),direction.toString());
        }
    }
    @Test void exactDuplicatesDisappearButPrismsAndDifferentPlanesStay(){
        var bed=top(0,0,1,1,.1f,65537,Direction.UP);var prism=top(0,0,1,1,.1f,17,Direction.UP);
        var raised=top(0,0,1,1,.2f,65538,Direction.UP);var back=top(0,0,1,1,.1f,65539,Direction.DOWN);
        var first=model(bed);var second=model(bed,prism,raised,back);
        CrystalBedModel.clean(List.of(first,second));
        assertEquals(List.of(bed),quads(first));assertEquals(List.of(prism,raised,back),quads(second));
    }
}
