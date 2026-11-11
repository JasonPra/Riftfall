package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;

/** Temporary unculled crystal rendering, without changing Minecraft's global terrain render state. */
public final class CrystalUnculledQuads {
    public static List<BakedQuad> twoSided(List<BakedQuad> source){
        var result=new ArrayList<BakedQuad>(source.size()*2);
        for(var quad:source){
            result.add(quad);var original=quad.getVertices();int stride=original.length/4;var data=original.clone();
            for(int v=0;v<4;v++){
                System.arraycopy(original,(3-v)*stride,data,v*stride,stride);
                int n=data[v*stride+7];data[v*stride+7]=(n&0xff000000)|((-((byte)n))&255)|((-((byte)(n>>8)))&255)<<8|((-((byte)(n>>16)))&255)<<16;
            }
            result.add(new BakedQuad(data,quad.getTintIndex(),quad.getDirection().getOpposite(),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion()));
        }return result;
    }
    private CrystalUnculledQuads(){}
}
