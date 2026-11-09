package org.Azkerash.riftfall.confidence;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WitnessedDeathMemoryTest {
    private void record(WitnessedDeathMemory memory,long now,WitnessSettings settings,UUID victim) {
        memory.remember(now,settings,victim,"example:ally",UUID.randomUUID(),"example:enemy");
    }
    @Test void lossesExpireIndependentlyWithoutRefreshingEarlierDeaths() {
        var memory=new WitnessedDeathMemory();var settings=new WitnessSettings(24,200,3);
        record(memory,100,settings,UUID.randomUUID());record(memory,150,settings,UUID.randomUUID());
        assertEquals(2,memory.active(299,3).size());assertEquals(1,memory.active(300,3).size());assertEquals(0,memory.active(350,3).size());
    }
    @Test void stackCapKeepsNewestLossesAndDuplicateEventsDoNotRefreshFear() {
        var memory=new WitnessedDeathMemory();var settings=new WitnessSettings(24,200,2);var victim=UUID.randomUUID();
        record(memory,0,settings,victim);record(memory,50,settings,victim);
        assertEquals(200,memory.active(50,2).getFirst().expiresAt());
        record(memory,100,settings,UUID.randomUUID());record(memory,150,settings,UUID.randomUUID());
        assertEquals(2,memory.active(200,2).size());assertEquals(1,memory.active(300,2).size());
    }
    @Test void disablingMemoryAndReducingCapTakeEffect() {
        var memory=new WitnessedDeathMemory();record(memory,0,new WitnessSettings(24,0,3),UUID.randomUUID());
        assertTrue(memory.active(0,3).isEmpty());
        record(memory,0,WitnessSettings.defaults(),UUID.randomUUID());assertTrue(memory.active(0,0).isEmpty());
    }
    @Test void separateObserversCanHaveDifferentDurationsAndCaps() {
        var shortLived=new WitnessedDeathMemory();var longLived=new WitnessedDeathMemory();var victim=UUID.randomUUID();
        record(shortLived,0,new WitnessSettings(24,20,1),victim);record(longLived,0,new WitnessSettings(24,200,3),victim);
        assertTrue(shortLived.active(20,1).isEmpty());assertEquals(1,longLived.active(20,3).size());
    }
    @Test void returnedMemoriesCannotBeMutated() {
        var memory=new WitnessedDeathMemory();record(memory,0,WitnessSettings.defaults(),UUID.randomUUID());
        assertThrows(UnsupportedOperationException.class,()->memory.active(0,3).clear());
    }
}
