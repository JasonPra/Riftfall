package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserLandscapeTest {
    @Test void riversFollowReliefWithShallowVariedBedsInsteadOfSeaLevelTrenches(){
        var g=new GeyserRegion(0,0,193,1000,1);
        int minDepth=99,maxDepth=0,minWater=999,maxWater=-999,drops=0;
        for(int z=-650;z<150;z++){
            int x=(int)Math.round(GeyserTerrain.riverCenter(g,z));
            var c=GeyserTerrain.column(42,g,x,z);
            assertTrue(c.wet(),"Dry river at "+z);
            if(c.kind()!=GeyserTerrain.WaterKind.RIVER)continue;
            int depth=c.water()-c.floor();minDepth=Math.min(minDepth,depth);maxDepth=Math.max(maxDepth,depth);
            minWater=Math.min(minWater,c.water());maxWater=Math.max(maxWater,c.water());
            assertTrue(GeyserTerrain.ground(g,x,z)-c.floor()<9,"River cuts a trench at "+z);
            int change=Math.abs(c.water()-GeyserTerrain.riverLevel(g,z-1));
            assertTrue(change<=2,"Abrupt disconnected reach");if(change>0)drops++;
        }
        assertTrue(minDepth>=1&&maxDepth<=4);assertTrue(maxDepth>minDepth);
        assertTrue(maxWater-minWater>=5);assertTrue(minWater>63);assertTrue(drops>0);
    }
    @Test void someRoutesEndInInternalLakesAndOthersReachTheBoundary(){
        int closed=0,open=0,lakes=0;
        for(int i=0;i<20;i++){
            var g=new GeyserRegion(i*512,-512,193,1000,1);
            for(boolean north:new boolean[]{false,true}){
                if(GeyserTerrain.openEnd(g,north)){open++;continue;}closed++;
                int z=(int)Math.round(g.centerZ()+(north?-1:1)*GeyserShape.radius(g)*.68);
                var c=GeyserTerrain.column(42,g,(int)Math.round(GeyserTerrain.riverCenter(g,z)),z);
                assertTrue(c.wet());assertEquals(GeyserTerrain.WaterKind.LAKE,c.kind());lakes++;
                int beyond=(int)Math.round(g.centerZ()+(north?-1:1)*GeyserShape.radius(g)*.9);
                var end=GeyserTerrain.column(42,g,(int)Math.round(GeyserTerrain.riverCenter(g,beyond)),beyond);
                assertNotEquals(GeyserTerrain.WaterKind.RIVER,end.kind());
            }
        }
        assertTrue(closed>0&&open>0&&lakes>0);
    }
    @Test void waterBiomesKeepZoneIdentityWithoutReplacingLandEverywhere(){
        var keys=new java.util.HashSet<>();
        for(var zone:new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER}){
            assertTrue(keys.add(GeyserBiomes.key(zone)));assertTrue(keys.add(GeyserBiomes.waterKey(zone,false)));assertTrue(keys.add(GeyserBiomes.waterKey(zone,true)));
        }
        assertEquals(12,keys.size());assertThrows(IllegalArgumentException.class,()->GeyserBiomes.waterKey(GeyserZone.OUTSIDE,true));
        var g=new GeyserRegion(-512,512,193,1000,1);
        int wet=0,dry=0;
        for(int z=(int)g.centerZ()-500;z<g.centerZ()+500;z+=4)for(int dx:new int[]{0,40}){
            int x=(int)Math.round(GeyserTerrain.riverCenter(g,z))+dx;var c=GeyserTerrain.column(42,g,x,z);
            var zone=GeyserShape.zone(g,x,z);
            assertEquals(c.wet()?GeyserBiomes.waterKey(zone,c.kind()==GeyserTerrain.WaterKind.LAKE):GeyserBiomes.key(zone),GeyserBiomes.surfaceKey(42,g,x,z));
            if(c.wet())wet++;else dry++;
        }
        assertTrue(wet>0&&dry>0);
    }
}
