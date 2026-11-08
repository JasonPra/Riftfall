package org.Azkerash.riftfall;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinTacticsTest {
    @Test void rangedCoverAlwaysFallsBehindTheMeleeFrontline() {
        Vec3 enemy=new Vec3(0,81,0),defender=new Vec3(4,81,0);
        assertEquals(new Vec3(10,81,0),GoblinTactics.behind(enemy,defender));
        assertEquals(new Vec3(-10,81,0),GoblinTactics.behind(enemy,new Vec3(-4,81,0)));
        assertEquals(81,GoblinTactics.behind(enemy,new Vec3(2,81,3)).y);
    }
    @Test void healingRequiresARealWoundAndLivingRecipient() {
        assertFalse(GoblinTactics.needsHealing(20,20));
        assertFalse(GoblinTactics.needsHealing(0,20));
        assertFalse(GoblinTactics.needsHealing(19,20));
        assertTrue(GoblinTactics.needsHealing(14,20));
    }
    @Test void rangedGoblinsRetreatFromCloseAttackers() {
        assertTrue(GoblinTactics.threatened(2*2));
        assertTrue(GoblinTactics.threatened(6*6));
        assertFalse(GoblinTactics.threatened(8*8));
    }
}
