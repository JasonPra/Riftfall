package org.Azkerash.riftfall;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinTacticsTest {
    @Test void plungesRequireNearbyHighGround() {
        assertTrue(GoblinTactics.canPlunge(1,3*3));
        assertFalse(GoblinTactics.canPlunge(0,3*3));
        assertFalse(GoblinTactics.canPlunge(-1,3*3));
        assertFalse(GoblinTactics.canPlunge(3,3*3));
        assertFalse(GoblinTactics.canPlunge(1,1));
        assertFalse(GoblinTactics.canPlunge(1,6*6));
    }
    @Test void rangedCoverAlwaysFallsBehindTheMeleeFrontline() {
        Vec3 enemy=new Vec3(0,81,0),defender=new Vec3(4,81,0);
        assertEquals(new Vec3(10,81,0),GoblinSquadTactics.rearSlot(enemy,defender,6,0));
        assertEquals(new Vec3(-10,81,0),GoblinSquadTactics.rearSlot(enemy,new Vec3(-4,81,0),6,0));
        assertEquals(81,GoblinSquadTactics.rearSlot(enemy,new Vec3(2,81,3),6,0).y);
    }
    @Test void healingRequiresARealWoundAndLivingRecipient() {
        assertFalse(GoblinTactics.needsHealing(20,20));
        assertFalse(GoblinTactics.needsHealing(0,20));
        assertFalse(GoblinTactics.needsHealing(19,20));
        assertTrue(GoblinTactics.needsHealing(14,20));
    }
}
