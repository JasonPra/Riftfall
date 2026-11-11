package org.Azkerash.riftfall.client;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalSurfaceRotationTest {
    @Test void surfaceTransformsPreserveBoundsAndMapTheGrowthAxisCorrectly(){
        int[] vertices=new int[32];for(int i=0;i<4;i++){vertices[i*8]=Float.floatToRawIntBits((i&1)==0?0:1);vertices[i*8+1]=Float.floatToRawIntBits(1);vertices[i*8+2]=Float.floatToRawIntBits((i&2)==0?0:1);}
        var original=new BakedQuad(vertices,-1,Direction.UP,null,false,false);
        for(var face:Direction.values()){
            var rotated=CrystalSurfaceRotation.rotate(original,face);assertEquals(face,rotated.getDirection());
            for(int i=0;i<4;i++)for(int axis=0;axis<3;axis++){float value=Float.intBitsToFloat(rotated.getVertices()[i*8+axis]);assertTrue(value>=0&&value<=1);}
            assertEquals(6,java.util.Arrays.stream(Direction.values()).map(d->CrystalSurfaceRotation.direction(d,face)).distinct().count());
        }
    }
}
