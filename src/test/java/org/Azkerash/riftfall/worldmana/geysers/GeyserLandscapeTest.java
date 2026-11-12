package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserLandscapeTest {
    @Test void gentleTerrainHasCrossZoneRiverRoutesAndSparseEntrances(){
        var g=new GeyserRegion(0,0,193,1000);int min=999,max=-999,entrances=0;
        for(int x=-1400;x<1400;x+=8)for(int z=-1400;z<1400;z+=8){
            int height=GeyserTerrain.height(42,g,x,z);min=Math.min(min,height);max=Math.max(max,height);
            assertTrue(height>45&&height<100);
            if(GeyserTerrain.entrance(42,g,x,height,z))entrances++;
        }
        assertTrue(min<63);assertTrue(max>75);assertTrue(entrances>0&&entrances<100);
        for(int z=-1400;z<1400;z++){
            int x=(int)Math.round(g.centerX()+75*Math.sin((z-g.centerZ())/210)+17*Math.sin((z-g.centerZ())/71));
            assertTrue(GeyserTerrain.height(42,g,x,z)<63,"Broken cross-ring river");
        }
    }
    @Test void networksOccupySeveralDepthsAndHaveVerticalLinks(){
        int links=0;
        for(int cell=0;cell<32;cell++){
            int x=cell*128+64,z=64;var network=GeyserCaveShape.network(42,x,z,44);
            assertTrue(network.layers().size()>=3);
            for(var layer:network.layers()){
                boolean open=false;for(int y=-24+layer.offset();y<=layer.offset();y++)open|=network.contains(y);
                assertTrue(open);
            }
            if(network.shafts().length>0){links++;for(int y=-50;y<=24;y++)assertTrue(network.contains(y));}
        }
        assertTrue(links>0);
        assertTrue(GeyserCaveShape.network(42,64,64,130).layers().size()>5);
    }
    @Test void eachRingHasADifferentRealBiomeKey(){
        var keys=new java.util.HashSet<>();
        for(var zone:new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER})assertTrue(keys.add(GeyserBiomes.key(zone)));
        assertThrows(IllegalArgumentException.class,()->GeyserBiomes.key(GeyserZone.OUTSIDE));
    }
}
