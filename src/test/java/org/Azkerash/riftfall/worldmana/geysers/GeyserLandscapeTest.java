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
            int x=(int)Math.round(GeyserTerrain.riverCenter(g,z));
            assertTrue(GeyserTerrain.height(42,g,x,z)<63,"Broken cross-ring river");
        }
    }
    @Test void junctionLakeHasOnlyTwoConnectedChannelExits(){
        var g=new GeyserRegion(0,0,193,1000);int centerZ=(int)GeyserTerrain.junctionZ(g),centerX=(int)GeyserTerrain.riverCenter(g,centerZ);
        int radius=288,step=4,n=radius*2/step+1;boolean[][] wet=new boolean[n][n],seen=new boolean[n][n];
        for(int x=0;x<n;x++)for(int z=0;z<n;z++)wet[x][z]=GeyserTerrain.height(42,g,centerX-radius+x*step,centerZ-radius+z*step)<63;
        var queue=new java.util.ArrayDeque<int[]>();queue.add(new int[]{n/2,n/2});
        while(!queue.isEmpty()){
            var p=queue.removeFirst();int x=p[0],z=p[1];if(x<0||z<0||x>=n||z>=n||seen[x][z]||!wet[x][z])continue;
            seen[x][z]=true;queue.add(new int[]{x-1,z});queue.add(new int[]{x+1,z});queue.add(new int[]{x,z-1});queue.add(new int[]{x,z+1});
        }
        var edge=new java.util.ArrayList<Boolean>();
        for(int x=0;x<n;x++)edge.add(seen[x][0]);for(int z=1;z<n;z++)edge.add(seen[n-1][z]);
        for(int x=n-2;x>=0;x--)edge.add(seen[x][n-1]);for(int z=n-2;z>0;z--)edge.add(seen[0][z]);
        int exits=0;for(int i=0;i<edge.size();i++)if(edge.get(i)&&!edge.get((i+edge.size()-1)%edge.size()))exits++;
        assertEquals(2,exits);assertTrue(GeyserTerrain.height(42,g,centerX,centerZ)<55);
        double min=100,max=0;for(int z=-1400;z<1400;z+=8){double w=GeyserTerrain.riverWidth(g,z);min=Math.min(min,w);max=Math.max(max,w);}
        assertTrue(max-min>5);
    }
    @Test void eachRingHasADifferentRealBiomeKey(){
        var keys=new java.util.HashSet<>();
        for(var zone:new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER})assertTrue(keys.add(GeyserBiomes.key(zone)));
        assertThrows(IllegalArgumentException.class,()->GeyserBiomes.key(GeyserZone.OUTSIDE));
    }
}
