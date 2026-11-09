package org.Azkerash.riftfall.confidence;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatStrengthTest {
    private double advantage(CombatStrength.Stats actor,CombatStrength.Stats target) {
        return ConfidenceEngine.evaluate(ConfidenceConfig.defaults().profile(),Map.of(),CombatStrength.factors(actor,target)).points();
    }
    @Test void betterArmorAndWeaponIntimidateAnUnarmoredWarrior() {
        var goblin=new CombatStrength.Stats(7,0,0,20);
        assertEquals(0,advantage(goblin,goblin));
        assertEquals(-18,advantage(goblin,new CombatStrength.Stats(10,15,0,20)));
        assertEquals(6,advantage(goblin,new CombatStrength.Stats(1,0,0,20)));
        assertEquals(18,advantage(new CombatStrength.Stats(10,15,0,20),goblin));
    }
    @Test void groupSupportCanOvercomeEquipmentDisadvantage() {
        var factors=new java.util.HashMap<>(CombatStrength.factors(new CombatStrength.Stats(7,0,0,20),new CombatStrength.Stats(10,15,0,20)));
        factors.put("base",1D);factors.put("equipment.sword",1D);
        var profile=ConfidenceConfig.defaults().profile();
        assertEquals(-10,ConfidenceEngine.evaluate(profile,Map.of(),factors).points());
        assertEquals(15,ConfidenceEngine.evaluate(profile,Map.of("riftfall:goblin",5D),factors).points());
    }
    @Test void extremeModdedAttributesAreBoundedAndHealthChangesMatter() {
        assertEquals(-47,advantage(new CombatStrength.Stats(0,0,0,1),new CombatStrength.Stats(1000,1000,1000,1000)));
        assertEquals(3,advantage(new CombatStrength.Stats(7,0,0,20),new CombatStrength.Stats(7,0,0,5)));
        assertThrows(IllegalArgumentException.class,()->new CombatStrength.Stats(Double.NaN,0,0,20));
    }
    @Test void relativeStrengthWeightsCanBeChangedPerSpeciesIncludingDisabled() {
        var tuned=EntityConfidenceTuning.apply(ConfidenceConfig.defaults(),Map.of("factor.combat.armor_advantage",0D,"factor.combat.damage_advantage",8D));
        var result=ConfidenceEngine.evaluate(tuned.profile(),Map.of(),CombatStrength.factors(new CombatStrength.Stats(7,0,0,20),new CombatStrength.Stats(10,15,0,20)));
        assertEquals(-6,result.points());
    }
}
