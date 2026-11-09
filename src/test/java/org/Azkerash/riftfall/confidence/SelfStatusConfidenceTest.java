package org.Azkerash.riftfall.confidence;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelfStatusConfidenceTest {
    private double penalty(boolean poison,boolean wither,int other) {
        return ConfidenceEngine.evaluate(ConfidenceConfig.defaults().profile(),Map.of(),
            CombatConfidence.selfStatusFactors(poison,wither,other)).points();
    }
    @Test void poisonAndWitherStackWithoutCountingTheirGenericPenaltyAgain() {
        assertEquals(-8,penalty(true,false,0));assertEquals(-10,penalty(false,true,0));
        assertEquals(-18,penalty(true,true,0));assertEquals(-26,penalty(true,true,2));
    }
    @Test void clearingEffectsRemovesTheirPenaltyAndOtherHarmfulTypesStack() {
        assertEquals(-8,penalty(false,false,2));assertEquals(-4,penalty(false,false,1));
        assertEquals(0,penalty(false,false,0));
        assertThrows(IllegalArgumentException.class,()->CombatConfidence.selfStatusFactors(false,false,-1));
    }
    @Test void existingConfigurationGainsNewPenaltiesWhileExplicitTuningWins() {
        var migrated=ConfidenceConfig.withStatusDefaults(Map.of("base",12D,"self.poison",0D,"self.wither",-3D));
        assertEquals(0,migrated.get("self.poison"));assertEquals(-3,migrated.get("self.wither"));
        assertEquals(-4,migrated.get("self.harmful_effect"));assertEquals(12,migrated.get("base"));
        assertEquals(-8,ConfidenceConfig.withStatusDefaults(Map.of()).get("self.poison"));
    }
    @Test void penaltiesCanChangeTheCombatDecisionWithoutWaitingForHealthLoss() {
        var settings=ConfidenceConfig.defaults();
        var healthy=ConfidenceEngine.evaluate(settings.profile(),Map.of("riftfall:goblin",4D),Map.of("base",1D,"equipment.sword",1D));
        var factors=new java.util.HashMap<>(Map.of("base",1D,"equipment.sword",1D));
        factors.putAll(CombatConfidence.selfStatusFactors(true,false,0));
        var poisoned=ConfidenceEngine.evaluate(settings.profile(),Map.of("riftfall:goblin",4D),factors);
        assertTrue(healthy.atLeast(settings.boldPoints()));assertFalse(poisoned.atLeast(settings.boldPoints()));
        assertEquals(healthy.points()-8,poisoned.points());
    }
}
