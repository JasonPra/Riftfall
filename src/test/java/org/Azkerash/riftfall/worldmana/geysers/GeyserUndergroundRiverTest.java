package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserUndergroundRiverTest {
    @Test void receivingPoolHasAnOpenDescendingShaftAndConnectedCaveReach(){
        GeyserRegion g=null;
        for(int i=0;i<32&&g==null;i++){var candidate=new GeyserRegion(i*512,0,193,1000,1);if(GeyserDrainage.of(candidate).underground())g=candidate;}
        assertNotNull(g);var route=GeyserDrainage.of(g);assertFalse(route.external());
        var river=GeyserUndergroundRiver.of(42,g);assertTrue(river.active());
        var mouth=route.mouth();var shaft=river.column((int)mouth.x(),(int)mouth.z());
        var flow=route.flow((int)mouth.x()+10,(int)mouth.z());assertTrue(flow.x()<-.8,"Receiving pool current misses the hole");
        assertNotNull(shaft);assertTrue(shaft.falling());assertTrue(shaft.wet());assertTrue(shaft.floor()<mouth.water()-6);assertTrue(shaft.roof()>=mouth.water());
        int previous=999;
        for(var point:river.path()){
            assertTrue(point.water()<=previous);previous=point.water();
            assertNotNull(river.column((int)point.x(),(int)point.z()));
        }
        assertTrue(previous<0,"River never reaches the lower cave network");
    }
    @Test void ordinarySurfaceCoursesDoNotGainAnUnplannedSinkhole(){
        for(int i=0;i<32;i++){
            var g=new GeyserRegion(i*512,512,129,1000,1);
            if(GeyserDrainage.of(g).underground())continue;
            var river=GeyserUndergroundRiver.of(42,g);assertFalse(river.active());assertNull(river.column((int)g.centerX(),(int)g.centerZ()));return;
        }
        fail("No surface course sampled");
    }
}
