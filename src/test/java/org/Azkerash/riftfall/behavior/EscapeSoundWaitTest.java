package org.Azkerash.riftfall.behavior;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EscapeSoundWaitTest {
    @Test void waitsForEveryObserverAndIgnoresCompletionOfAnOlderClip(){
        var wait=new EscapeSoundWait();var first=UUID.randomUUID();var second=UUID.randomUUID();
        assertTrue(wait.started(first,1,0,100));assertTrue(wait.started(first,2,10,100));
        assertTrue(wait.started(second,3,10,100));assertFalse(wait.ready(20));
        wait.finished(first,1);wait.finished(second,3);assertFalse(wait.ready(26));
        wait.finished(first,2);assertTrue(wait.ready(27));
    }
    @Test void missingOrDisconnectedObserversExpireWithoutHoldingTheMobForever(){
        var wait=new EscapeSoundWait();assertTrue(wait.started(UUID.randomUUID(),1,0,140));
        assertFalse(wait.ready(50));assertFalse(wait.ready(139));assertTrue(wait.ready(140));
    }
    @Test void aShortStateDeliveryGraceAcceptsAnInFlightStartButStopsFurtherRepeats(){
        var wait=new EscapeSoundWait();var observer=UUID.randomUUID();
        assertFalse(wait.ready(50));assertTrue(wait.started(observer,1,54,100));
        assertFalse(wait.started(observer,2,56,100));wait.finished(observer,1);assertTrue(wait.ready(56));
        wait.resetHold();assertTrue(wait.started(observer,2,100,100));
        assertFalse(wait.started(observer,3,100,141));assertFalse(wait.started(observer,0,100,20));
    }
}
