package org.Azkerash.riftfall.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.Azkerash.riftfall.crystals.CrystalSurfaceConnections;

/** Exact quarter-turn attachment transforms; UVs and winding remain unchanged. */
public final class CrystalSurfaceRotation {
    public static Direction direction(Direction local,Direction face){
        var x=CrystalSurfaceConnections.tangent(face,1);var z=CrystalSurfaceConnections.tangent(face,2);
        int nx=x.getStepX()*local.getStepX()+face.getStepX()*local.getStepY()+z.getStepX()*local.getStepZ(),ny=x.getStepY()*local.getStepX()+face.getStepY()*local.getStepY()+z.getStepY()*local.getStepZ(),nz=x.getStepZ()*local.getStepX()+face.getStepZ()*local.getStepY()+z.getStepZ()*local.getStepZ();
        for(var d:Direction.values())if(d.getStepX()==nx&&d.getStepY()==ny&&d.getStepZ()==nz)return d;
        throw new IllegalArgumentException("Invalid surface rotation");
    }
    public static BakedQuad rotate(BakedQuad quad,Direction face){
        if(face==Direction.UP)return quad;var data=quad.getVertices().clone();int stride=data.length/4;var x=CrystalSurfaceConnections.tangent(face,1);var z=CrystalSurfaceConnections.tangent(face,2);
        for(int vertex=0;vertex<4;vertex++){
            int offset=vertex*stride;double a=Float.intBitsToFloat(data[offset])-.5,b=Float.intBitsToFloat(data[offset+1])-.5,c=Float.intBitsToFloat(data[offset+2])-.5;
            data[offset]=Float.floatToRawIntBits((float)(.5+a*x.getStepX()+b*face.getStepX()+c*z.getStepX()));data[offset+1]=Float.floatToRawIntBits((float)(.5+a*x.getStepY()+b*face.getStepY()+c*z.getStepY()));data[offset+2]=Float.floatToRawIntBits((float)(.5+a*x.getStepZ()+b*face.getStepZ()+c*z.getStepZ()));
            int normal=data[offset+7],aN=(byte)normal,bN=(byte)(normal>>8),cN=(byte)(normal>>16),nx=aN*x.getStepX()+bN*face.getStepX()+cN*z.getStepX(),ny=aN*x.getStepY()+bN*face.getStepY()+cN*z.getStepY(),nz=aN*x.getStepZ()+bN*face.getStepZ()+cN*z.getStepZ();data[offset+7]=(normal&0xff000000)|(nx&255)|(ny&255)<<8|(nz&255)<<16;
        }
        return new BakedQuad(data,quad.getTintIndex(),direction(quad.getDirection(),face),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion());
    }
    private CrystalSurfaceRotation(){}
}
