package org.Azkerash.riftfall;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GrappleStruggleTest {
    @Test void ambushRequiresTheRearArc() {
        var look=new net.minecraft.world.phys.Vec3(0,0,1);
        assertTrue(GoblinGrapple.isBehind(look,new net.minecraft.world.phys.Vec3(0,0,-1)));
        assertFalse(GoblinGrapple.isBehind(look,new net.minecraft.world.phys.Vec3(0,0,1)));
        assertFalse(GoblinGrapple.isBehind(look,new net.minecraft.world.phys.Vec3(1,0,0)));
    }
    @Test void sixDistinctPressesEscapeAndPacketBurstsDoNot() {
        var struggle=new GrappleStruggle();
        assertTrue(struggle.press(100));
        for (int i=0;i<20;i++) assertFalse(struggle.press(100));
        assertFalse(struggle.press(101));
        assertEquals(1,struggle.progress());
        for (int tick=102;tick<=110;tick+=2) assertTrue(struggle.press(tick));
        assertTrue(struggle.complete());
        assertFalse(struggle.press(112));
        struggle.reset(); assertFalse(struggle.complete()); assertEquals(0,struggle.progress());
    }
    @Test void exactlyFourStabsLandBeforeTheHoldEnds() {
        int stabs=0;
        for (int tick=0;tick<80;tick++) if (GrappleStruggle.stabsAt(tick)) stabs++;
        assertEquals(4,stabs);
        assertFalse(GrappleStruggle.stabsAt(0)); assertFalse(GrappleStruggle.stabsAt(84));
    }
    @Test void swipeBackswingAndFlurryHaveDistinctImpactsAndRecovery() {
        for (var attack : GoblinTactics.SwordAttack.values()) {
            int hits=0;
            for (int tick=0;tick<=attack.duration;tick++) if (attack.hitsAt(tick)) hits++;
            assertEquals(switch(attack) { case FLURRY -> 3; case TWOHAND_FLURRY -> 4; default -> 1; },hits);
            assertFalse(attack.hitsAt(0)); assertFalse(attack.hitsAt(attack.duration));
        }
    }
}
