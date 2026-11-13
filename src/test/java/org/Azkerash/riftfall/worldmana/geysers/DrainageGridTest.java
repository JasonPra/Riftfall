package org.Azkerash.riftfall.worldmana.geysers;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DrainageGridTest {
    @Test void divertsAroundRidgeThroughLowestAvailablePass(){
        int n=9;double[] terrain=new double[n*n];Arrays.fill(terrain,4);
        for(int x=0;x<n;x++)terrain[4*n+x]=30;
        terrain[4*n+7]=6;int source=n+1,goal=7*n+1;
        var route=DrainageGrid.solve(n,terrain,goal);boolean pass=false;
        for(int i=source;i!=goal;i=route.parent()[i]){
            assertTrue(route.parent()[i]>=0);assertTrue(terrain[i]<30,"Route crosses ridge instead of the pass");
            if(i==4*n+7)pass=true;
        }
        assertTrue(pass);assertEquals(6,route.spill()[source]);
    }
    @Test void depressionFillsToSpillwayThenDrainsWithoutUphillEdgesOrCycles(){
        int n=9;double[] terrain=new double[n*n];Arrays.fill(terrain,20);
        for(int z=2;z<=6;z++)for(int x=2;x<=6;x++)terrain[z*n+x]=3;
        terrain[7*n+4]=8;terrain[8*n+4]=1;
        var route=DrainageGrid.solve(n,terrain,8*n+4);
        assertEquals(8,route.spill()[4*n+4]);
        int steps=0;
        for(int i=4*n+4;route.parent()[i]>=0;i=route.parent()[i]){
            assertTrue(route.spill()[route.parent()[i]]<=route.spill()[i]);assertTrue(++steps<=n*n);
        }
        assertTrue(steps>0);
    }
    @Test void anotherLowOutletPreventsAnArtificialBoundaryDam(){
        int n=7;var ground=new double[n*n];Arrays.fill(ground,4);ground[0]=30;
        var result=DrainageGrid.solve(n,ground,new int[]{0,n*n-1},Double.NEGATIVE_INFINITY);
        assertEquals(4,result.spill()[n*3+3]);
        int i=n*3+3;while(result.parent()[i]>=0)i=result.parent()[i];assertEquals(n*n-1,i);
    }
    @Test void rejectsUnusableOutlets(){assertThrows(IllegalArgumentException.class,()->DrainageGrid.solve(1,new double[]{Double.POSITIVE_INFINITY},0));}
}
