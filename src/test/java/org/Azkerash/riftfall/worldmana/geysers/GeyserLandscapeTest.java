package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserLandscapeTest {
    @Test void removingWaterwaysPreservesTheExistingLandElevationProfile(){
        for(var g:new GeyserRegion[]{new GeyserRegion(0,0,193,1000,1),new GeyserRegion(-512,512,129,1000,1)})
            for(int dx=-1500;dx<=1500;dx+=31)for(int dz=-1500;dz<=1500;dz+=31){
                double expected=77+7*Math.sin(dx/170.0)+5*Math.cos(dz/210.0)+3*Math.sin((dx+dz)/87.0);
                assertEquals((int)Math.floor(expected),GeyserTerrain.height(42,g,(int)g.centerX()+dx,(int)g.centerZ()+dz));
            }
    }
    @Test void landBiomesRemainDistinctAndLegacyWaterBiomeKeysRemainLoadable(){
        var keys=new java.util.HashSet<>();
        for(var zone:new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER}){
            assertTrue(keys.add(GeyserBiomes.key(zone)));assertTrue(keys.add(GeyserBiomes.waterKey(zone,false)));assertTrue(keys.add(GeyserBiomes.waterKey(zone,true)));
        }
        assertEquals(12,keys.size());assertThrows(IllegalArgumentException.class,()->GeyserBiomes.key(GeyserZone.OUTSIDE));
    }
}
