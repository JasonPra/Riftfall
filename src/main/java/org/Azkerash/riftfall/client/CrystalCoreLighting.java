package org.Azkerash.riftfall.client;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;

/** Only inset core surfaces bypass environmental lighting. Shells keep their original light/shading. */
public final class CrystalCoreLighting {
    public static boolean core(BakedQuad quad){return quad.getSprite()!=null&&quad.getSprite().contents().name().getPath().endsWith("_mana_crystal_core");}
    public static BakedQuad fullbright(BakedQuad quad){
        var vertices=quad.getVertices();int stride=vertices.length/4;boolean ready=!quad.isShade()&&!quad.hasAmbientOcclusion();for(int i=0;i<4;i++)ready&=vertices[i*stride+6]==LightTexture.FULL_BRIGHT;if(ready)return quad;
        var copy=vertices.clone();for(int i=0;i<4;i++)copy[i*stride+6]=LightTexture.FULL_BRIGHT;
        return new BakedQuad(copy,quad.getTintIndex(),quad.getDirection(),quad.getSprite(),false,false);
    }
    private CrystalCoreLighting(){}
}
