package org.Azkerash.riftfall.client;
import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalUnculledQuadsTest {
    @Test void allOriginalFacesStayAndGetAnOppositelyWoundCopy(){
        int[] data=new int[32];for(int i=0;i<32;i++)data[i]=i;var q=new BakedQuad(data,7,Direction.NORTH,null,false,false);
        var result=CrystalUnculledQuads.twoSided(List.of(q));assertEquals(2,result.size());assertSame(q,result.getFirst());var back=result.getLast();assertEquals(Direction.SOUTH,back.getDirection());assertEquals(7,back.getTintIndex());
        for(int v=0;v<4;v++)for(int a=0;a<7;a++)assertEquals(data[(3-v)*8+a],back.getVertices()[v*8+a]);
        assertArrayEquals(java.util.stream.IntStream.range(0,32).toArray(),data);
    }
}
