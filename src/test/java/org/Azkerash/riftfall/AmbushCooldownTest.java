package org.Azkerash.riftfall;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AmbushCooldownTest {
    @Test void recoveryStartsAfterTheAnimationAndNeverShortensAnExistingCooldown(){
        assertEquals(380,AmbushCooldown.deadline(0,100,80,200));
        assertEquals(338,AmbushCooldown.deadline(0,100,38,200));
        assertEquals(900,AmbushCooldown.deadline(900,100,80,200));
        assertEquals(3_000_000_280L,AmbushCooldown.deadline(0,3_000_000_000L,80,200));
    }
    @Test void customRecoveryCanBeZeroButNegativeTimingsAreRejected(){
        assertEquals(180,AmbushCooldown.deadline(0,100,80,0));
        assertThrows(IllegalArgumentException.class,()->AmbushCooldown.deadline(0,100,-1,200));
        assertThrows(IllegalArgumentException.class,()->AmbushCooldown.deadline(0,100,80,-1));
    }
    @Test void reusableGrappleGeometryAndRecoveryAreValidated(){
        var defaults=GrappleGoal.Settings.defaults();assertEquals(200,defaults.cooldownTicks());assertEquals(16,defaults.cooldownRadius());
        assertDoesNotThrow(()->new GrappleGoal.Settings(2,.7,.9,12,100,24));
        assertThrows(IllegalArgumentException.class,()->new GrappleGoal.Settings(Double.NaN,.4,.5,8,200,16));
        assertThrows(IllegalArgumentException.class,()->new GrappleGoal.Settings(1.5,.4,.5,0,200,16));
        assertThrows(IllegalArgumentException.class,()->new GrappleGoal.Settings(1.5,.4,.5,8,-1,16));
    }
}
