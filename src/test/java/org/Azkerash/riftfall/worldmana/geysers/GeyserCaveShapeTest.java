package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserCaveShapeTest {
    private boolean opening(long seed,int x,int z){for(int y=-32;y<=32;y++)if(GeyserCaveShape.contains(seed,x,y,z))return true;return false;}
    @Test void tunnelsConnectAcrossEveryRingAndNegativeChunkBoundaries(){
        for(long seed:new long[]{1,42,-781})for(int axis=0;axis<2;axis++)for(int coordinate=-2200;coordinate<=2200;coordinate+=4)
            assertTrue(opening(seed,axis==0?coordinate:64,axis==0?64:coordinate),"Disconnected ring route at "+coordinate);
    }
    @Test void cavernsAreLargeButRetainSolidSpaceAndBedrock(){
        for(long seed:new long[]{1,42,-781}){
            int width=0,height=0;
            for(int x=0;x<128;x++)if(opening(seed,x,64))width++;
            for(int y=-52;y<=44;y++)if(GeyserCaveShape.contains(seed,64,y,64))height++;
            assertTrue(width>=76);assertTrue(height>=40);
            assertFalse(GeyserCaveShape.contains(seed,64,-60,64));assertFalse(GeyserCaveShape.contains(seed,64,60,64));
            assertFalse(opening(seed,0,0));
        }
    }
}
