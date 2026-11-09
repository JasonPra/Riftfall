package org.Azkerash.riftfall.confidence;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntityConfidenceTuningTest {
    @Test void speciesOverridesAreIndependentAndPreserveUntouchedWeights() {
        var base=ConfidenceConfig.defaults();
        var timid=EntityConfidenceTuning.apply(base,Map.of("factor.self.witnessed_ally_death",-15D,"factor.self.poison",-12D,"boldPoints",30D,"unit.riftfall:goblin",2D));
        var bold=EntityConfidenceTuning.apply(base,Map.of("factor.self.witnessed_ally_death",0D,"boldPoints",10D));
        assertEquals(-15,timid.profile().factorWeights().get("self.witnessed_ally_death"));
        assertEquals(0,bold.profile().factorWeights().get("self.witnessed_ally_death"));
        assertEquals(-8,base.profile().factorWeights().get("self.witnessed_ally_death"));
        assertEquals(2,timid.profile().unitWeights().get("riftfall:goblin"));
        assertEquals(base.profile().factorWeights().get("equipment.sword"),timid.profile().factorWeights().get("equipment.sword"));
    }
    @Test void thresholdsAndNormalizationCanBeTunedAlongWithWitnessLifetime() {
        var tuned=EntityConfidenceTuning.apply(ConfidenceConfig.defaults(),Map.of("normalizationPoints",100D,"woundedHealthFraction",.5D,"vulnerableTargetHealthFraction",.2D,"retreatingAllyMultiplier",0D));
        assertEquals(100,tuned.profile().normalizationPoints());assertEquals(.5,tuned.woundedFraction());assertEquals(.2,tuned.vulnerableFraction());assertEquals(0,tuned.retreatingMultiplier());
        var witness=EntityConfidenceTuning.witness(WitnessSettings.defaults(),Map.of("witnessDurationTicks",40D,"witnessMaxStacks",1D,"witnessRadius",12D));
        assertEquals(new WitnessSettings(12,40,1),witness);
    }
    @Test void invalidTuningIsRejectedRatherThanProducingBrokenScores() {
        assertFalse(EntityConfidenceTuning.valid("unknown",1));assertFalse(EntityConfidenceTuning.valid("witnessDurationTicks",1.5));
        assertFalse(EntityConfidenceTuning.valid("factor.self.poison",Double.NaN));assertFalse(EntityConfidenceTuning.valid("woundedHealthFraction",2));
        assertThrows(IllegalArgumentException.class,()->EntityConfidenceTuning.apply(ConfidenceConfig.defaults(),Map.of("unit.example:ally",-1D)));
    }
}
