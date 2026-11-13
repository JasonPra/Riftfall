package org.Azkerash.riftfall.client.wood;

import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.Azkerash.riftfall.wood.LegacyBarkWound;

/** Clip DT's axis-aligned branch faces at block boundaries, preserving winding and UVs. */
final class LegacyBarkPatches {
    static List<BakedQuad> split(BakedQuad quad,long patches,TextureAtlasSprite exposed){
        Direction face=quad.getDirection();
        long selected=patches&LegacyBarkWound.facePatches(face);
        if(selected==0)return List.of(quad);
        int u=face.getAxis()==Direction.Axis.X?2:0,v=face.getAxis()==Direction.Axis.Y?2:1;
        int[] data=quad.getVertices();int stride=data.length/4;
        List<float[]> original=new ArrayList<>();
        for(int i=0;i<4;i++){
            float[] point=new float[5];
            for(int a=0;a<3;a++)point[a]=Float.intBitsToFloat(data[i*stride+a]);
            point[3]=Float.intBitsToFloat(data[i*stride+4]);point[4]=Float.intBitsToFloat(data[i*stride+5]);
            original.add(point);
        }
        List<BakedQuad> result=new ArrayList<>();
        for(int row=-1;row<=1;row++)for(int col=-1;col<=1;col++){
            var points=clip(clip(clip(clip(original,u,col,true),u,col+1,false),v,row,true),v,row+1,false);
            if(points.size()!=4||area(points,u,v)<1e-7)continue;
            double[] middle=new double[3];middle[u]=col+.5;middle[v]=row+.5;
            boolean strip=(selected&LegacyBarkWound.patch(face,middle[0],middle[1],middle[2]))!=0;
            int[] vertices=data.clone();var bark=quad.getSprite();
            for(int i=0;i<4;i++){
                float[] point=points.get(i);
                for(int a=0;a<3;a++)vertices[i*stride+a]=Float.floatToRawIntBits(point[a]);
                float texU=point[3],texV=point[4];
                if(strip){
                    texU=exposed.getU0()+(texU-bark.getU0())/(bark.getU1()-bark.getU0())*(exposed.getU1()-exposed.getU0());
                    texV=exposed.getV0()+(texV-bark.getV0())/(bark.getV1()-bark.getV0())*(exposed.getV1()-exposed.getV0());
                }
                vertices[i*stride+4]=Float.floatToRawIntBits(texU);vertices[i*stride+5]=Float.floatToRawIntBits(texV);
            }
            result.add(new BakedQuad(vertices,quad.getTintIndex(),face,strip?exposed:bark,quad.isShade(),quad.hasAmbientOcclusion()));
        }
        return result;
    }
    private static double area(List<float[]> points,int u,int v){
        double sum=0;for(int i=0;i<points.size();i++){var a=points.get(i);var b=points.get((i+1)%points.size());sum+=a[u]*b[v]-b[u]*a[v];}return Math.abs(sum)*.5;
    }
    private static List<float[]> clip(List<float[]> points,int axis,float edge,boolean lower){
        if(points.isEmpty())return points;
        List<float[]> result=new ArrayList<>();var previous=points.getLast();boolean was=lower?previous[axis]>=edge:previous[axis]<=edge;
        for(var current:points){
            boolean inside=lower?current[axis]>=edge:current[axis]<=edge;
            if(inside!=was){
                float t=(edge-previous[axis])/(current[axis]-previous[axis]);float[] intersection=new float[5];
                for(int i=0;i<5;i++)intersection[i]=previous[i]+t*(current[i]-previous[i]);
                result.add(intersection);
            }
            if(inside)result.add(current);
            previous=current;was=inside;
        }
        // A boundary passing through a corner can emit duplicate vertices.
        for(int i=result.size()-1;i>=0&&result.size()>1;i--){
            var a=result.get(i);var b=result.get((i+1)%result.size());
            if(Math.abs(a[0]-b[0])+Math.abs(a[1]-b[1])+Math.abs(a[2]-b[2])<1e-6)result.remove(i);
        }
        return result;
    }
    private LegacyBarkPatches(){}
}
