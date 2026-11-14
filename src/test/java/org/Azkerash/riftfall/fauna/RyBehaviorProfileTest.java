package org.Azkerash.riftfall.fauna;
import java.util.Map;
import org.Azkerash.riftfall.behavior.BehaviorContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RyBehaviorProfileTest {
    private String choose(Map<String,Double> signals) {
        return RyBehaviorProfile.engine().selectAction(new BehaviorContext(signals),null);
    }
    @Test void braveryDoesNotMeanFightingToDeath() {
        assertFalse(RyBehaviorProfile.shouldRetreat(.6,-20,false,true));
        assertFalse(RyBehaviorProfile.shouldRetreat(.4,15,false,true));
        assertTrue(RyBehaviorProfile.shouldRetreat(.2,30,false,true));
        assertTrue(RyBehaviorProfile.shouldRetreat(.4,-20,false,true));
        assertTrue(RyBehaviorProfile.shouldRetreat(1,30,true,true));
        assertFalse(RyBehaviorProfile.shouldRetreat(.1,-20,false,false));
    }
    @Test void selfPreservationBeatsStartingAnAttack() {
        assertEquals("retreat",choose(Map.of("ready.retreat",1D,"ready.cast",1D,"ready.bite",1D)));
        assertEquals("interrupted",choose(Map.of("interrupted",1D,"ready.retreat",1D,"ready.cast",1D,"committed.cast",1D)));
    }
    @Test void committedBiteFinishesWithoutFlickeringIntoAnotherAction() {
        assertEquals("bite",choose(Map.of("ready.bite",1D,"committed.bite",1D,"ready.cast",1D)));
    }
    @Test void warningAndGuardingPreemptIdlePatrol() {
        assertEquals("warn",choose(Map.of("ready.warn",1D,"ready.patrol",1D)));
        assertEquals("guard",choose(Map.of("ready.guard",1D,"ready.patrol",1D,"ready.rest",1D)));
        assertEquals("rest",choose(Map.of("ready.rest",1D,"ready.patrol",1D)));
    }
}
