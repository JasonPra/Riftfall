package org.Azkerash.riftfall.client;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
/** Non-rendered occlusion geometry for the neighbor's continuous root at a shared edge. */
public final class CrystalBorderMask {
    public static final int ID=Integer.MIN_VALUE;
    public static BakedQuad make(Direction side,Direction growth,double thickness,TextureAtlasSprite sprite){
        int axis=side.getAxis().ordinal(); // Axis enum is X,Y,Z.
        int root=growth.getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;double[] min={0,0,0},max={1,1,1};boolean positive=growth.getStepX()+growth.getStepY()+growth.getStepZ()>0;
        if(positive)max[root]=thickness;else min[root]=1-thickness;double plane=side.getStepX()+side.getStepY()+side.getStepZ()>0?1:0;
        int[] data=new int[32];int[][] corners={{0,0},{1,0},{1,1},{0,1}};
        for(int i=0;i<4;i++){int j=plane==1?i:3-i;double[] p=new double[3];p[axis]=plane;p[u]=corners[j][0]==0?min[u]:max[u];p[v]=corners[j][1]==0?min[v]:max[v];for(int a=0;a<3;a++)data[i*8+a]=Float.floatToRawIntBits((float)p[a]);data[i*8+3]=-1;}
        return new BakedQuad(data,ID,side,sprite,false,false);
    }
    private CrystalBorderMask(){}
}
