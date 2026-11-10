package org.Azkerash.riftfall;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BackstabTimingTest {
    @Test void aLethalHitWaitsForBothTheAttackerAndTheVictimsEntireFall(){
        assertEquals(150,BackstabTiming.fatalResolution(150,105,38));
        assertEquals(151,BackstabTiming.fatalResolution(150,111,40));
        assertEquals(160,BackstabTiming.fatalResolution(138,122,38));
    }
    @Test void downedProtectionEndsOnlyWhenGetupStartsAndNeverDuringPendingDeath(){
        for(boolean great:new boolean[]{false,true}){
            long getup=100+BackstabTiming.getupDelayTicks(great);
            assertFalse(BackstabTiming.vulnerable(true,false,getup-1,getup));assertTrue(BackstabTiming.vulnerable(true,false,getup,getup));
            assertFalse(BackstabTiming.vulnerable(true,true,getup+100,getup));assertFalse(BackstabTiming.vulnerable(false,false,getup,getup));
        }
    }
    @Test void holdingADeferredDeathNeverRestoresHealthOrAppliesLethalDamageEarly(){
        for(float hp:new float[]{.25F,1,4,20}){float damage=BackstabTiming.initialLethalDamage(hp);assertTrue(damage>=0 && damage<hp);assertTrue(hp-damage<=1);}
    }
}
