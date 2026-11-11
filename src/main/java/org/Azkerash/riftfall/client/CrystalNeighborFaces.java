package org.Azkerash.riftfall.client;

import net.minecraft.client.renderer.block.model.BakedQuad;

/** Temporary clipping masks from the canonical owning neighbor; never rendered. */
public final class CrystalNeighborFaces {
    public static final int MASK_ID=Integer.MIN_VALUE;
    public static BakedQuad mask(BakedQuad source,int dx,int dy,int dz){
        var vertices=source.getVertices();int stride=vertices.length/4;int[] offset={dx,dy,dz};
        for(int axis=0;axis<3;axis++){
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(int v=0;v<4;v++){double value=Float.intBitsToFloat(vertices[v*stride+axis])+offset[axis];min=Math.min(min,value);max=Math.max(max,value);}
            if(max<-.125||min>1.125)return null;
        }
        var data=vertices.clone();for(int v=0;v<4;v++)for(int axis=0;axis<3;axis++)data[v*stride+axis]=Float.floatToRawIntBits(Float.intBitsToFloat(data[v*stride+axis])+offset[axis]);
        return new BakedQuad(data,MASK_ID,source.getDirection(),source.getSprite(),source.isShade(),source.hasAmbientOcclusion());
    }
    public static BakedQuad solidFace(net.minecraft.core.Direction face,int dx,int dy,int dz){
        int axis=face.getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;boolean positive=face.getStepX()+face.getStepY()+face.getStepZ()>0;
        int[] data=new int[32];int[][] corners={{0,0},{1,0},{1,1},{0,1}};
        for(int i=0;i<4;i++){int j=positive?i:3-i;float[] p=new float[3];p[axis]=positive?1:0;p[u]=corners[j][0];p[v]=corners[j][1];for(int a=0;a<3;a++)data[i*8+a]=Float.floatToRawIntBits(p[a]);}
        return mask(new BakedQuad(data,MASK_ID,face,null,false,false),dx,dy,dz);
    }
    private CrystalNeighborFaces(){}
}
