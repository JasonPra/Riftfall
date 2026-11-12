package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RiverCurrentFieldTest {
    private boolean water(int x,int z){return Math.abs(z)<=8&&Math.abs(x)<=240||x>=80&&x<=240&&Math.abs(z)<=100||x<=-80&&x>=-160&&Math.abs(z)<=32;}
    @Test void levelRiverMovesAwayFromTheLargerConnectedBody(){
        var flow=RiverCurrentField.solve(0,0,(x,z)->water(x,z)?63:RiverCurrentField.DRY);
        assertTrue(flow.x()<-.5);assertEquals(0,flow.z(),.01);
    }
    @Test void downhillOutletOverridesAwayFromWaterRule(){
        var flow=RiverCurrentField.solve(0,0,(x,z)->water(x,z)?(x>=80?60:70):RiverCurrentField.DRY);
        assertTrue(flow.x()>.5);
    }
    @Test void currentTurnsAlongTheChannelInsteadOfPushingIntoADryBank(){
        var flow=RiverCurrentField.solve(0,0,(x,z)->x>=0&&x<=240&&Math.abs(z)<=3||z<=0&&z>=-240&&Math.abs(x)<=3||x>=80&&x<=240&&Math.abs(z)<=100?63:RiverCurrentField.DRY);
        assertTrue(flow.z()<-.5);assertEquals(0,flow.x(),.01);
    }
    @Test void disconnectedWaterDoesNotAttractTheRoute(){
        var flow=RiverCurrentField.solve(0,0,(x,z)->water(x,z)||z>=200&&z<=600&&Math.abs(x)<500?63:RiverCurrentField.DRY);
        assertTrue(flow.x()<-.5);
        assertEquals(new RiverCurrentField.Flow(0,0),RiverCurrentField.solve(0,0,(x,z)->RiverCurrentField.DRY));
    }
}
