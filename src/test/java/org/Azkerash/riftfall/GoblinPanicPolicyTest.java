package org.Azkerash.riftfall;

import java.util.Map;
import org.Azkerash.riftfall.behavior.BehaviorContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GoblinPanicPolicyTest {
    @Test void escapeWinsOverCommittedAttacksButHitStunStillInterrupts(){
        var engine=GoblinBehaviorProfile.engine();
        assertEquals("panic_escape",engine.select(new BehaviorContext(Map.of("ready.panic_escape",1D,"ready.melee",1D,"committed.melee",1D)),"melee").selected().orElseThrow());
        assertEquals("interrupted",engine.select(new BehaviorContext(Map.of("ready.panic_escape",1D,"self.interrupted",1D)),"panic_escape").selected().orElseThrow());
    }
    @Test void riftRestrictionFallsBackToExistingRetreatWhilePanicSignalRemains(){
        var engine=GoblinBehaviorProfile.engine();
        assertEquals("warrior_retreat",engine.select(new BehaviorContext(Map.of("panic.active",1D,"panic.escape_allowed",0D,"ready.warrior_retreat",1D)),"panic_escape").selected().orElseThrow());
    }
}
