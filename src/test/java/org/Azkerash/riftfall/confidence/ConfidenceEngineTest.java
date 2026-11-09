package org.Azkerash.riftfall.confidence;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfidenceEngineTest {
    @Test void arbitraryMobTypesAndFutureCombatFactorsNeedNoEngineChanges() {
        var profile=new ConfidenceProfile(Map.of("example:knight",7D,"example:captain",13D),
            Map.of("equipment.spear",3D,"self.afraid_of_fire",-12D),40);
        var result=ConfidenceEngine.evaluate(profile,Map.of("example:knight",2D,"example:captain",1D),
            Map.of("equipment.spear",1D,"self.afraid_of_fire",1D));
        assertEquals(18,result.points());assertEquals(.45,result.value(),.0001);
        assertEquals(14,result.contributions().get("allies.example:knight"));
        assertEquals(-12,result.contributions().get("factor.self.afraid_of_fire"));
    }
    @Test void everyUnitAddsItsWeightWithoutAHiddenHeadcountCap() {
        var profile=ConfidenceConfig.defaults().profile();
        var result=ConfidenceEngine.evaluate(profile,Map.of("riftfall:goblin",3D,"riftfall:goblin_archer",2D,"riftfall:goblin_shaman",1D),Map.of());
        assertEquals(27,result.points());
        assertEquals(50,ConfidenceEngine.evaluate(profile,Map.of("riftfall:goblin",10D),Map.of()).points());
    }
    @Test void personalLoadoutIsSeparateFromAlliedUnitWeight() {
        var profile=ConfidenceConfig.defaults().profile();
        assertEquals(8,ConfidenceEngine.evaluate(profile,Map.of(),Map.of("base",1D,"equipment.sword",1D)).points());
        assertEquals(11,ConfidenceEngine.evaluate(profile,Map.of(),Map.of("base",1D,"equipment.sword_shield",1D)).points());
        assertEquals(2,ConfidenceEngine.evaluate(profile,Map.of(),Map.of("base",1D,"equipment.shield_only",1D)).points());
        assertEquals(0,ConfidenceEngine.evaluate(profile,Map.of(),Map.of("base",1D,"equipment.unarmed",1D)).points());
    }
    @Test void retreatingAlliesContributeAFractionOfTheirNormalWeight() {
        var settings=ConfidenceConfig.defaults();
        var result=ConfidenceEngine.evaluate(settings.profile(),Map.of("riftfall:goblin",settings.retreatingMultiplier()),Map.of());
        assertEquals(1.25,result.points());
    }
    @Test void situationalBonusesStackAndWoundsCanUndoBoldness() {
        var settings=ConfidenceConfig.defaults();
        var result=ConfidenceEngine.evaluate(settings.profile(),Map.of(),Map.of("base",1D,"equipment.sword",1D,
            "self.low_health",1D,"target.low_health",1D,"target.poison",1D,"target.wither",1D));
        assertEquals(25,result.points());assertTrue(result.atLeast(settings.boldPoints()));
        var healthy=ConfidenceEngine.evaluate(settings.profile(),Map.of("riftfall:goblin",4D),Map.of("base",1D,"equipment.sword",1D));
        var wounded=ConfidenceEngine.evaluate(settings.profile(),Map.of("riftfall:goblin",4D),Map.of("base",1D,"equipment.sword",1D,"self.low_health",1D));
        assertTrue(healthy.atLeast(settings.boldPoints()));assertFalse(wounded.atLeast(settings.boldPoints()));
    }
    @Test void rawPointsRemainVisibleWhileOnlyNormalizedConfidenceIsClamped() {
        var profile=ConfidenceConfig.defaults().profile();
        var scared=ConfidenceEngine.evaluate(profile,Map.of(),Map.of("equipment.unarmed",1D,"self.low_health",1D));
        assertEquals(-14,scared.points());assertEquals(0,scared.value());
        var supported=ConfidenceEngine.evaluate(profile,Map.of("riftfall:goblin",100D),Map.of());
        assertEquals(500,supported.points());assertEquals(1,supported.value());
    }
    @Test void independentProfilesAndUnconfiguredUnitsDoNotLeakIntoEachOther() {
        var one=new ConfidenceProfile(Map.of("example:unit",5D),Map.of(),20);
        var two=new ConfidenceProfile(Map.of("example:unit",1D),Map.of(),20);
        assertEquals(5,ConfidenceEngine.evaluate(one,Map.of("example:unit",1D,"example:unknown",100D),Map.of()).points());
        assertEquals(1,ConfidenceEngine.evaluate(two,Map.of("example:unit",1D),Map.of()).points());
    }
    @Test void profilesAndResultsAreImmutableAndInvalidValuesAreRejected() {
        var weights=new HashMap<String,Double>();weights.put("example:unit",5D);
        var profile=new ConfidenceProfile(weights,Map.of(),40);weights.put("example:unit",100D);
        var result=ConfidenceEngine.evaluate(profile,Map.of("example:unit",1D),Map.of());
        assertEquals(5,result.points());
        assertThrows(UnsupportedOperationException.class,()->result.contributions().clear());
        assertThrows(IllegalArgumentException.class,()->new ConfidenceProfile(Map.of(),Map.of(),0));
        assertThrows(IllegalArgumentException.class,()->ConfidenceEngine.evaluate(profile,Map.of("example:unit",Double.NaN),Map.of()));
    }
}
