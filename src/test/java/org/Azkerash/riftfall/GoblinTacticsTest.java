package org.Azkerash.riftfall;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinTacticsTest {
    @Test void flurryStunCoversSuccessiveCutsButEndsWithTheAnimation() {
        var attack=GoblinTactics.SwordAttack.TWOHAND_FLURRY;
        int last=-1;
        for (int tick=0;tick<attack.duration;tick++) if (attack.hitsAt(tick)) {
            if (last>=0) assertTrue((tick-last)/20F<GoblinFlurryStun.chainTime(0));
            last=tick;
        }
        assertEquals(attack.duration/20F,last/20F+GoblinFlurryStun.chainTime(0),.001F);
        assertEquals(.8F,GoblinFlurryStun.chainTime(.8F));
    }
    @Test void goblinSwordAndOffhandChooseTheMovesetWithoutAffectingOtherWeapons() {
        for (int combo=0;combo<3;combo++) {
            assertEquals(GoblinTactics.SwordAttack.TWOHAND_FLURRY,GoblinSwordCombat.select(true,true,false,combo));
            assertNotEquals(GoblinTactics.SwordAttack.TWOHAND_FLURRY,GoblinSwordCombat.select(false,true,false,combo));
            assertNotEquals(GoblinTactics.SwordAttack.TWOHAND_FLURRY,GoblinSwordCombat.select(true,false,false,combo));
        }
        assertEquals(GoblinTactics.SwordAttack.SWIPE,GoblinSwordCombat.select(true,false,true,0));
        assertEquals(GoblinTactics.SwordAttack.BACKSWING,GoblinSwordCombat.select(true,false,true,1));
        assertEquals(GoblinTactics.SwordAttack.SHIELD_OVERHEAD,GoblinSwordCombat.select(true,false,true,2));
        assertEquals(GoblinTactics.SwordAttack.FLURRY,GoblinSwordCombat.select(false,false,true,2));
    }
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
