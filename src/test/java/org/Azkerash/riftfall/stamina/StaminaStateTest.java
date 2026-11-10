package org.Azkerash.riftfall.stamina;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaminaStateTest {
    @Test void startsFullAndChargesOnlyOncePerSuccessfulAction(){
        var s=new StaminaState();assertEquals(40,s.current(40));
        assertTrue(s.spend(12,40,20,"flurry"));assertEquals(28,s.current(40));assertFalse(s.panting());
        assertEquals("flurry",s.lastExpense());assertEquals(20,s.recoveryDelay());
    }
    @Test void unaffordableActionCannotOverdrawAndForcesPanting(){
        var s=new StaminaState();s.restore(3,false,0,40);
        assertFalse(s.spend(6,40,20,"swipe"));assertEquals(3,s.current(40));assertTrue(s.panting());
        assertFalse(s.spend(1,40,20,"cheap skill"));assertEquals(3,s.current(40));
    }
    @Test void pantingOverridesDelayAndBonusesAndLastsUntilFull(){
        var s=new StaminaState();s.restore(0,true,20,40);
        for(int i=0;i<99;i++)s.tick(40,4,true,true,true);
        assertTrue(s.panting());assertEquals(39.6,s.current(40),1E-8);
        // Floating point accumulation may leave a few ulps: tick must snap to full.
        s.tick(40,4,true,true,true);assertFalse(s.panting());assertEquals(40,s.current(40),1E-8);
        assertEquals(0,s.recoveryDelay());
    }
    @Test void adrenalineUsesFifteenAndTwentyFivePercentWithoutStacking(){
        assertEquals(1,StaminaState.regenerationMultiplier(false,false,false));
        assertEquals(1.15,StaminaState.regenerationMultiplier(false,false,true));
        assertEquals(1.25,StaminaState.regenerationMultiplier(false,true,false));
        assertEquals(1.25,StaminaState.regenerationMultiplier(false,true,true));
        assertEquals(2,StaminaState.regenerationMultiplier(true,true,true));
        var bold=new StaminaState();bold.restore(0,false,0,40);
        var fleeing=new StaminaState();fleeing.restore(0,false,0,40);
        for(int i=0;i<20;i++){bold.tick(40,4,false,true,false);fleeing.tick(40,4,true,false,false);}
        assertEquals(4.6,bold.current(40),1E-8);assertEquals(5,fleeing.current(40),1E-8);
    }
    @Test void expenditureDelaysPassiveRegenerationAndWorkingPreventsIt(){
        var s=new StaminaState();s.spend(10,40,20,"attack");
        for(int i=0;i<20;i++)s.tick(40,4,false,false,false);
        assertEquals(30,s.current(40));s.tick(40,4,false,false,true);assertEquals(30,s.current(40));
        s.tick(40,4,false,false,false);assertEquals(30.2,s.current(40),1E-8);
    }
    @Test void sprintingConsumesPerTickAndEventuallyRequiresRecovery(){
        var s=new StaminaState();int paid=0;
        while(s.spend(.2,40,20,"sprint")&&paid<1000)paid++;
        assertEquals(200,paid);assertTrue(s.panting());assertEquals(0,s.current(40),1E-8);
    }
    @Test void persistenceRestoresExhaustionAndHandlesConfigurationChanges(){
        var original=new StaminaState();original.restore(7,true,13,40);
        var loaded=new StaminaState();loaded.restore(original.current(40),original.panting(),original.recoveryDelay(),40);
        assertEquals(7,loaded.current(40));assertTrue(loaded.panting());assertEquals(13,loaded.recoveryDelay());
        assertEquals(5,loaded.current(5));loaded.restore(Double.NaN,false,0,40);assertEquals(40,loaded.current(40));
    }
}
