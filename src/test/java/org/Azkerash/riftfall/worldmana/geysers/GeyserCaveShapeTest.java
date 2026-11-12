package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserCaveShapeTest {
    @Test void junctionsVaryInElevationAndSizeInsteadOfRepeatingAFloorPlan(){
        var heights=new java.util.HashSet<Double>();int narrow=0,broad=0,dry=0;
        for(int cell=-12;cell<12;cell++){
            var n=GeyserCaveShape.node(42,cell,0,0);heights.add(n.y());int width=0;
            for(int dx=-36;dx<=36;dx++)if(GeyserCaveShape.network(42,(int)n.x()+dx,(int)n.z(),80).contains((int)n.y()))width++;
            assertTrue(GeyserCaveShape.network(42,(int)n.x(),(int)n.z(),80).contains((int)n.y()));
            if(width<16)narrow++;if(width>30)broad++;
            if(!GeyserCaveShape.network(42,(int)n.x(),(int)n.z(),80).water((int)n.y()))dry++;
        }
        assertTrue(heights.size()>12);assertTrue(narrow>0&&broad>0&&dry>0);
    }
    @Test void someShaftsAreFloodedEvenAboveTheDeepWaterTable(){
        int flooded=0,dry=0;
        for(int x=-16;x<16;x++)for(int z=0;z<4;z++){
            var low=GeyserCaveShape.node(42,x,-1,z);if((low.hash()&3072)!=0)continue;
            var high=GeyserCaveShape.node(42,x,0,z);
            var cave=GeyserCaveShape.network(42,(int)high.x(),(int)high.z(),100);
            assertTrue(cave.contains((int)high.y()));
            if((low.hash()&16384)==0){assertTrue(cave.water((int)high.y()));flooded++;}
            else if(!cave.water((int)high.y()))dry++;
        }
        assertTrue(flooded>0&&dry>0);
    }
    @Test void cachedAndUncachedQueriesAgreeAcrossNegativeChunkEdges(){
        for(int x:new int[]{-113,-112,-17,-16,-1,0,15,16,111,112}){
            var first=GeyserCaveShape.network(73,x,-17,58);boolean[] before=new boolean[118];
            for(int y=-59;y<59;y++)before[y+59]=first.contains(y);
            for(int k=0;k<8;k++)GeyserCaveShape.network(73,x+256*k,900,100);
            var again=GeyserCaveShape.network(73,x,-17,58);
            for(int y=-59;y<59;y++)assertEquals(before[y+59],again.contains(y));
            assertFalse(again.contains(-60));assertFalse(again.contains(59));
        }
    }
}
