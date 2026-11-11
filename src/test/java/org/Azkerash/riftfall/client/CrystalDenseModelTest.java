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
import org.Azkerash.riftfall.crystals.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalDenseModelTest {
    @Test void packingPreservesEveryFaceUvAndDepthOnAllSixGrowthFaces(){
        var input=new ArrayList<BakedQuad>();
        for(var face:Direction.values()){
            int axis=face.getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;boolean positive=face.getStepX()+face.getStepY()+face.getStepZ()>0;int[] data=new int[32];int[][] corners={{0,0},{1,0},{1,1},{0,1}};
            for(int i=0;i<4;i++){int j=positive?i:3-i;float[] p=new float[3];p[axis]=positive?.75f:.25f;p[u]=corners[j][0]==0?.25f:.75f;p[v]=corners[j][1]==0?.25f:.75f;for(int a=0;a<3;a++)data[i*8+a]=Float.floatToRawIntBits(p[a]);data[i*8+3]=-1;data[i*8+4]=Float.floatToRawIntBits(i/4f);data[i*8+5]=Float.floatToRawIntBits(i/5f);data[i*8+6]=1234;}
            input.add(new BakedQuad(data,17,face,null,true,true));
        }
        BakedModel raw=new BakedModelWrapper<BakedModel>(null){@Override public List<BakedQuad> getQuads(BlockState s,Direction side,RandomSource r,ModelData d,RenderType type){return side==null?input:List.of();}};
        for(var growth:Direction.values()){
            var model=new CrystalDenseModel(raw,null,growth,CrystalAdultDensity.profile(growth,p->true),1);
            var output=model.getQuads(null,null,RandomSource.create(0),ModelData.EMPTY,null);assertEquals(input.size(),output.size());
            for(int i=0;i<input.size();i++){
                var before=input.get(i);var after=output.get(i);assertEquals(before.getDirection(),after.getDirection());assertEquals(17,after.getTintIndex());
                for(int v=0;v<4;v++)for(int axis=0;axis<3;axis++){double p=Float.intBitsToFloat(after.getVertices()[v*8+axis]);assertTrue(p>=0&&p<=1);if(axis==growth.getAxis().ordinal())assertEquals(before.getVertices()[v*8+axis],after.getVertices()[v*8+axis]);}
                for(int v=0;v<4;v++)for(int a=3;a<8;a++)assertEquals(before.getVertices()[v*8+a],after.getVertices()[v*8+a]);
                var d=after.getVertices();double[] a=new double[3],b=new double[3];for(int k=0;k<3;k++){a[k]=Float.intBitsToFloat(d[8+k])-Float.intBitsToFloat(d[k]);b[k]=Float.intBitsToFloat(d[24+k])-Float.intBitsToFloat(d[k]);}
                var face=after.getDirection();double dot=(a[1]*b[2]-a[2]*b[1])*face.getStepX()+(a[2]*b[0]-a[0]*b[2])*face.getStepY()+(a[0]*b[1]-a[1]*b[0])*face.getStepZ();assertTrue(dot>0,"Packing must preserve front-facing winding");
            }
        }
    }
}
