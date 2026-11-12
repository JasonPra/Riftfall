package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserCaveShapeTest {
    private boolean opening(long seed,int x,int z){for(int y=-54;y<=30;y++)if(GeyserCaveShape.contains(seed,x,y,z))return true;return false;}
    @Test void windingTunnelsConnectAcrossEveryRingAndNegativeChunkBoundaries(){
        for(long seed:new long[]{1,42,-781})for(int axis=0;axis<2;axis++)for(int coordinate=-2200;coordinate<=2200;coordinate++){
            double t=Math.floorMod(coordinate-64,128)/128.0;
            int bend=(int)Math.round(12*Math.sin(t*Math.PI*2));
            assertTrue(opening(seed,axis==0?coordinate:64-bend,axis==0?64+bend:coordinate),"Disconnected winding route at "+coordinate);
        }
    }
    @Test void modestChambersAreOccasionalAndHabitatsIncludeWaterAndLushSegments(){
        boolean wet=false,lush=false,river=false,shaft=false;int chambers=0;
        for(int cell=0;cell<64;cell++){
            int x=cell*128+64,z=64,top=-100,bottom=100;
            var column=GeyserCaveShape.column(42,x,z);
            for(int y=-55;y<=30;y++)if(column.contains(y)){top=Math.max(top,y);bottom=Math.min(bottom,y);}
            assertTrue(top-bottom<50);if(top-bottom>=15)chambers++;
            wet|=column.floodLevel()!=Integer.MIN_VALUE;lush|=column.lush();
            river|=column.streamLevel()!=Integer.MIN_VALUE;shaft|=bottom< -45;
            assertFalse(GeyserCaveShape.contains(42,x,-60,z));assertFalse(GeyserCaveShape.contains(42,x,60,z));
            assertFalse(opening(42,x+64,z+64));
        }
        assertTrue(chambers>0&&chambers<32);assertTrue(wet&&lush&&river&&shaft);
    }
}
