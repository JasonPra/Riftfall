package org.Azkerash.riftfall.behavior;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BehaviorEngineTest {
    private BehaviorContext combat(double confidence,double distance,boolean wounded) {
        return new BehaviorContext(Map.of("confidence",confidence,"fear",1-confidence,"self.wounded",wounded?1D:0D,
            "target.visible",1D,"target.distance",distance));
    }
    private String selected(BehaviorContext context) { return BehaviorTestProfile.engine().select(context,null).selected().orElseThrow(); }
    @Test void sameThreatProducesDifferentActionsAsConfidenceChanges() {
        assertEquals("retreat",selected(combat(.2,4,false)));
        assertEquals("approach",selected(combat(.8,4,false)));
        assertEquals("attack",selected(combat(.8,1,false)));
    }
    @Test void cowardKeepsSafeDistanceInsteadOfImmediatelyApproachingAgain() {
        assertEquals("watch",selected(combat(.2,9,false)));
        assertEquals("watch",selected(combat(1,9,true)));
        assertEquals("retreat",selected(combat(.2,7,false)));
    }
    @Test void severeWoundsOutweighNumericalConfidence() {
        assertEquals("attack",selected(combat(1,1,false)));
        assertEquals("retreat",selected(combat(1,1,true)));
    }
    @Test void lostSightSelectsSearchAndExpiredMemorySelectsIdle() {
        var engine=BehaviorTestProfile.engine();
        assertEquals("search",engine.select(new BehaviorContext(Map.of("target.remembered",1D)),"attack").selected().orElseThrow());
        assertEquals("idle",engine.select(new BehaviorContext(Map.of()),"search").selected().orElseThrow());
    }
    @Test void unavailableAttackCannotWinRegardlessOfItsWeight() {
        var engine=new BehaviorEngine(List.of(
            new BehaviorRule("attack",1000,Map.of(),c->c.active("contact")),
            new BehaviorRule("wait",1,Map.of(),c->true)),0);
        assertEquals("wait",engine.select(new BehaviorContext(Map.of()),"attack").selected().orElseThrow());
    }
    @Test void inertiaStopsFlickerButDoesNotBlockMeaningfulChanges() {
        var engine=new BehaviorEngine(List.of(
            new BehaviorRule("a",10,Map.of(),c->true),
            new BehaviorRule("b",0,Map.of("opportunity",1D),c->true)),2);
        assertEquals("a",engine.select(new BehaviorContext(Map.of("opportunity",11D)),"a").selected().orElseThrow());
        assertEquals("b",engine.select(new BehaviorContext(Map.of("opportunity",13D)),"a").selected().orElseThrow());
    }
    @Test void customSignalsAndNegativeWeightsWorkWithoutChangingEngine() {
        var engine=new BehaviorEngine(List.of(
            new BehaviorRule("rescue",0,Map.of("friend.in_danger",30D,"fire",-40D),c->true),
            new BehaviorRule("guard",10,Map.of(),c->true)),0);
        assertEquals("rescue",engine.select(new BehaviorContext(Map.of("friend.in_danger",1D)),null).selected().orElseThrow());
        assertEquals("guard",engine.select(new BehaviorContext(Map.of("friend.in_danger",1D,"fire",1D)),null).selected().orElseThrow());
    }
    @Test void resultsAreIndependentPerActorAndExposeRawCandidateScores() {
        var engine=BehaviorTestProfile.engine();
        var brave=engine.select(combat(.8,4,false),null);
        var afraid=engine.select(combat(.2,4,false),null);
        assertEquals("approach",brave.selected().orElseThrow());assertEquals("retreat",afraid.selected().orElseThrow());
        assertEquals(39D,afraid.scores().get("retreat"),.0001);
        assertFalse(brave.scores().containsKey("retreat"));
        assertThrows(UnsupportedOperationException.class,()->afraid.scores().put("attack",100D));
    }
    @Test void noEligibleActionIsExplicitAndTiesFollowProfileOrder() {
        var unavailable=new BehaviorRule("a",0,Map.of(),c->false);
        assertTrue(new BehaviorEngine(List.of(unavailable),0).select(new BehaviorContext(Map.of()),null).selected().isEmpty());
        var engine=new BehaviorEngine(List.of(new BehaviorRule("a",1,Map.of(),c->true),new BehaviorRule("b",1,Map.of(),c->true)),0);
        assertEquals("a",engine.select(new BehaviorContext(Map.of()),null).selected().orElseThrow());
        assertEquals("b",engine.select(new BehaviorContext(Map.of()),"b").selected().orElseThrow());
    }
    @Test void explanationsIncludeRejectedCandidatesAndTheirInputs() {
        var decision=BehaviorTestProfile.engine().select(combat(.8,4,false),"approach");
        var rejected=decision.evaluations().get("retreat");
        assertFalse(rejected.eligible());assertEquals(30,rejected.weights().get("fear"));assertEquals(6,rejected.terms().get("fear"),.0001);
        assertEquals(21,rejected.total(),.0001);assertEquals(2,decision.switchMargin());assertEquals("approach",decision.previous());
    }
    @Test void invalidProfilesAndInputsFailEarly() {
        var rule=new BehaviorRule("a",0,Map.of(),c->true);
        assertThrows(IllegalArgumentException.class,()->new BehaviorEngine(List.of(rule,rule),0));
        assertThrows(IllegalArgumentException.class,()->new BehaviorEngine(List.of(rule),-1));
        assertThrows(IllegalArgumentException.class,()->new BehaviorContext(Map.of("bad",Double.NaN)));
        assertThrows(IllegalArgumentException.class,()->new BehaviorRule("a",0,Map.of("bad",Double.POSITIVE_INFINITY),c->true));
    }
}
