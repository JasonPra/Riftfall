package org.Azkerash.riftfall.client;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalCoreLightingTest {
    @Test void fullbrightOnlyChangesTheCoreCopy(){var original=new BakedQuad(new int[32],-1,Direction.UP,null,true,true);var core=CrystalCoreLighting.fullbright(original);for(int i=0;i<4;i++){assertEquals(LightTexture.FULL_BRIGHT,core.getVertices()[i*8+6]);assertEquals(0,original.getVertices()[i*8+6]);}assertFalse(core.isShade());assertFalse(core.hasAmbientOcclusion());assertTrue(original.isShade());}
    @Test void rotationsPreserveCoreLightAndShadingFlags(){var core=CrystalCoreLighting.fullbright(new BakedQuad(new int[32],-1,Direction.UP,null,true,true));for(var direction:Direction.values()){var rotated=CrystalSurfaceRotation.rotate(core,direction);for(int i=0;i<4;i++)assertEquals(0xf000f0,rotated.getVertices()[i*8+6]);assertFalse(rotated.isShade());assertFalse(rotated.hasAmbientOcclusion());}}
    @Test void alreadyEmissiveCoresAreReused(){var core=CrystalCoreLighting.fullbright(new BakedQuad(new int[32],-1,Direction.UP,null,true,true));assertSame(core,CrystalCoreLighting.fullbright(core));}
}
