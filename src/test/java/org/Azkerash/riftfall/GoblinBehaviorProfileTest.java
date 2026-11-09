package org.Azkerash.riftfall;

import java.util.Map;
import org.Azkerash.riftfall.behavior.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinBehaviorProfileTest {
    private String choose(Map<String,Double> signals) {
        return GoblinBehaviorProfile.engine().select(new BehaviorContext(signals),null).selected().orElseThrow();
    }
    @Test void warriorAbilitiesAndRetreatCanPreemptOrdinaryPursuit() {
        assertEquals("melee",choose(Map.of("ready.melee",1D)));
        assertEquals("plunge",choose(Map.of("ready.melee",1D,"ready.plunge",1D)));
        assertEquals("grapple",choose(Map.of("ready.melee",1D,"ready.plunge",1D,"ready.grapple",1D)));
        assertEquals("warrior_retreat",choose(Map.of("ready.melee",1D,"ready.grapple",1D,"ready.warrior_retreat",1D,"fear",1D)));
    }
    @Test void committedSwingsFinishButStunAlwaysInterrupts() {
        var signals=new java.util.HashMap<>(Map.of("ready.melee",1D,"committed.melee",1D,"ready.warrior_retreat",1D));
        assertEquals("melee",choose(signals));signals.put("self.interrupted",1D);assertEquals("interrupted",choose(signals));
    }
    @Test void shamanKeepsStaffHealingAndSpellOptionsWithProperCommitment() {
        assertEquals("cast",choose(Map.of("ready.cast",1D,"ready.position",1D)));
        assertEquals("heal",choose(Map.of("ready.cast",1D,"ready.heal",1D)));
        assertEquals("staff",choose(Map.of("ready.staff",1D,"ready.heal",1D,"ready.ranged_retreat",1D)));
        assertEquals("cast",choose(Map.of("ready.staff",1D,"ready.cast",1D,"committed.cast",1D)));
    }
    @Test void archersRepositionAndCoverRetreatingWarriors() {
        assertEquals("position",choose(Map.of("ready.position",1D,"ready.bow",1D)));
        assertEquals("bow",choose(Map.of("ready.position",1D,"ready.bow",1D,"cover.safe",1D)));
        assertEquals("ranged_retreat",choose(Map.of("ready.ranged_retreat",1D,"ready.bow",1D,"fear",1D)));
    }
    @Test void fearfulWarriorWatchesOrRetreatsInsteadOfAdvancing() {
        assertEquals("watch",choose(Map.of("self.melee_role",1D,"target.visible",1D,"engage",0D,"fear",1D)));
        assertEquals("cautious_retreat",choose(Map.of("self.melee_role",1D,"target.visible",1D,"engage",0D,"ready.cautious_retreat",1D,"fear",1D)));
        assertEquals("melee",choose(Map.of("self.melee_role",1D,"target.visible",1D,"engage",1D,"ready.melee",1D)));
    }
    @Test void lostSightUsesMemoryInsteadOfLaunchingNewAttacks() {
        assertEquals("search",choose(Map.of("target.remembered",1D)));
        assertEquals("idle",choose(Map.of("target.remembered",1D,"self.melee_role",1D,"engage",0D)));
        assertEquals("search",choose(Map.of("target.remembered",1D,"self.melee_role",1D,"engage",1D)));
        assertEquals("idle",choose(Map.of()));assertEquals("wander",choose(Map.of("ready.wander",1D)));
    }
}
