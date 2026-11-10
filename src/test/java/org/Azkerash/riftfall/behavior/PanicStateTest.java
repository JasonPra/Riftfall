package org.Azkerash.riftfall.behavior;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanicStateTest {
    private final PanicSettings settings=PanicSettings.defaults();
    private PanicState panicked(){var state=new PanicState();for(int tick=0;tick<20;tick++)state.observe(tick,-30,true,true,settings);assertTrue(state.active());return state;}
    @Test void sustainedThreatEntersPanicButBriefDipsAndNoThreatDoNot(){
        var state=new PanicState();state.observe(0,-30,true,true,settings);state.observe(18,-30,true,true,settings);assertFalse(state.active());
        state.observe(19,0,true,true,settings);state.observe(20,-30,true,true,settings);assertFalse(state.active());
        state.observe(100,-30,false,true,settings);assertFalse(state.active());
        assertTrue(panicked().active());
    }
    @Test void recoveryRequiresHysteresisAndStableConfidence(){
        var state=panicked();state.observe(120,4,true,true,settings);assertTrue(state.active());
        state.observe(121,10,true,true,settings);state.observe(179,10,true,true,settings);assertTrue(state.active());
        state.observe(180,10,true,true,settings);assertFalse(state.active());
    }
    @Test void escapingRequiresTheWholeDiggingDuration(){
        var state=panicked();assertTrue(state.beginBurrow(20,true,true));
        assertFalse(state.advanceBurrow(69,true,true,settings));assertTrue(state.active());
        assertTrue(state.advanceBurrow(70,true,true,settings));assertEquals(PanicState.Stage.ESCAPED,state.stage());
    }
    @Test void environmentBlocksEscapeButKeepsFearAndCanCancelAtTheLastTick(){
        var state=panicked();assertFalse(state.beginBurrow(20,true,false));assertTrue(state.active());
        assertTrue(state.beginBurrow(21,true,true));assertFalse(state.advanceBurrow(71,true,false,settings));
        assertEquals(PanicState.Stage.PANICKED,state.stage());assertTrue(state.active());
    }
    @Test void completedDigWaitsUndergroundUntilAudioFinishesWithoutEmergingWhenApproached(){
        var state=panicked();state.beginBurrow(20,true,true);
        assertFalse(state.advanceBurrow(70,true,true,settings,false));
        assertEquals(PanicState.Stage.UNDERGROUND,state.stage());assertTrue(state.active());assertEquals(1,state.progress(90,settings));
        assertFalse(state.advanceBurrow(90,false,true,settings,false));
        assertTrue(state.advanceBurrow(100,false,true,settings,true));assertEquals(PanicState.Stage.ESCAPED,state.stage());
    }
    @Test void riftRestrictionStillCancelsAnUndergroundAudioWait(){
        var state=panicked();state.beginBurrow(20,true,true);
        state.advanceBurrow(70,true,true,settings,false);
        assertFalse(state.advanceBurrow(80,true,false,settings,false));assertEquals(PanicState.Stage.PANICKED,state.stage());
    }
    @Test void theLastDiggingTickMustStillBeSafeBeforeCommittingUnderground(){
        var state=panicked();state.beginBurrow(20,true,true);
        assertFalse(state.advanceBurrow(70,false,true,settings,false));
        assertEquals(PanicState.Stage.PANICKED,state.stage());assertEquals(0,state.progress(70,settings));
    }
    @Test void damageOrLostSupportRestartsTheFullDiggingWindow(){
        var state=panicked();state.beginBurrow(20,true,true);
        assertFalse(state.advanceBurrow(69,false,true,settings));assertEquals(0,state.progress(69,settings));
        state.beginBurrow(70,true,true);assertFalse(state.advanceBurrow(71,true,true,settings));
        assertTrue(state.advanceBurrow(120,true,true,settings));
    }
    @Test void speciesEligibilityAndTuningAreIndependent(){
        var state=panicked();state.observe(21,-30,true,false,settings);assertFalse(state.active());
        var custom=settings.overrides(Map.of("panicEntryTicks",1D,"panicBurrowTicks",20D,"panicEscapeDistance",16D));
        state.observe(22,-30,true,true,custom);assertTrue(state.active());state.beginBurrow(23,true,true);
        assertFalse(state.advanceBurrow(42,true,true,custom));assertTrue(state.advanceBurrow(43,true,true,custom));
        assertEquals(50,settings.burrowTicks());assertEquals(16,custom.escapeDistance());
        assertFalse(PanicSettings.validOverride("panicBurrowTicks",1));assertFalse(PanicSettings.validOverride("panicFleeSpeed",Double.NaN));
    }
}
