package org.Azkerash.riftfall.client.wood;

import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.Azkerash.riftfall.wood.LegacyBarkWound;

/** Clip DT's axis-aligned branch faces at block boundaries, preserving winding and UVs. */
final class LegacyBarkPatches {
    static List<BakedQuad> split(BakedQuad quad,long patches,TextureAtlasSprite exposed,int radius,int sections){
        Direction face=quad.getDirection();
        long selected=face.getAxis()==Direction.Axis.Y?patches:patches&LegacyBarkWound.facePatches(face);
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
        double minimum=.5-radius/16.0,width=radius/8.0;
        float[] horizontal=sections==0?new float[]{-1,0,1,2}:new float[sections+1];
        if(sections>0)for(int i=0;i<=sections;i++)horizontal[i]=(float)(minimum+width*i/sections);
        float[] vertical=face.getAxis()==Direction.Axis.Y?horizontal:new float[]{-1,0,1,2};
        for(int row=0;row<vertical.length-1;row++)for(int col=0;col<horizontal.length-1;col++){
            var points=clip(clip(clip(clip(original,u,horizontal[col],true),u,horizontal[col+1],false),v,vertical[row],true),v,vertical[row+1],false);
            if(points.size()!=4||area(points,u,v)<1e-7)continue;
            double[] middle=new double[3];middle[u]=(horizontal[col]+horizontal[col+1])*.5;middle[v]=(vertical[row]+vertical[row+1])*.5;
            boolean strip;
            if(face.getAxis()==Direction.Axis.Y){
                // Bark ledges follow the nearest adjoining wall patch, including corners.
                double dx=middle[0]-.5,dz=middle[2]-.5;
                Direction owner=Direction.getNearest(dx,0,dz);
                strip=(selected&LegacyBarkWound.patch(owner,middle[0],.5,middle[2],radius,sections))!=0;
                strip|=(selected&LegacyBarkWound.patch(face,middle[0],middle[1],middle[2]))!=0;
                if(Math.abs(Math.abs(dx)-Math.abs(dz))<1e-5){
                    Direction other=owner.getAxis()==Direction.Axis.X?(dz<0?Direction.NORTH:Direction.SOUTH):(dx<0?Direction.WEST:Direction.EAST);
                    strip|=(selected&LegacyBarkWound.patch(other,middle[0],.5,middle[2],radius,sections))!=0;
                }
            }else strip=(selected&LegacyBarkWound.patch(face,middle[0],middle[1],middle[2],radius,sections))!=0;
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
