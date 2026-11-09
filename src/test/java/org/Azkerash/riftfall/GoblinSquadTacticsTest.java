package org.Azkerash.riftfall;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinSquadTacticsTest {
    @Test void weightedSupportControlsCourageAndRetreatInsteadOfWarriorCount() {
        var settings=org.Azkerash.riftfall.confidence.ConfidenceConfig.defaults();
        var inputs=java.util.Map.of("base",1D,"equipment.bow",1D);
        var alone=org.Azkerash.riftfall.confidence.ConfidenceEngine.evaluate(settings.profile(),java.util.Map.of(),inputs);
        var warriors=org.Azkerash.riftfall.confidence.ConfidenceEngine.evaluate(settings.profile(),java.util.Map.of("riftfall:goblin",4D),inputs);
        var leaders=org.Azkerash.riftfall.confidence.ConfidenceEngine.evaluate(settings.profile(),java.util.Map.of("riftfall:goblin_shaman",2D),inputs);
        var mixed=org.Azkerash.riftfall.confidence.ConfidenceEngine.evaluate(settings.profile(),java.util.Map.of("riftfall:goblin_shaman",1D,"riftfall:goblin",2D),inputs);
        assertFalse(alone.atLeast(settings.boldPoints()));
        assertTrue(warriors.atLeast(settings.boldPoints()));
        assertEquals(warriors.points(),leaders.points());assertEquals(warriors.points(),mixed.points());
        assertTrue(GoblinSquadTactics.fleeDistance(warriors.value())<GoblinSquadTactics.fleeDistance(alone.value()));
        assertTrue(GoblinSquadTactics.retreatHealth(warriors.value())<GoblinSquadTactics.retreatHealth(alone.value()));
    }
    @Test void screenAndRearSlotsSandwichTheWarriorBetweenEnemyAndArcher() {
        Vec3 enemy=new Vec3(0,80,0),archer=new Vec3(10,80,0);
        Vec3 screen=GoblinSquadTactics.screen(enemy,archer,2.5);
        assertEquals(new Vec3(2.5,80,0),screen);
        assertEquals(new Vec3(6.5,80,2),GoblinSquadTactics.rearSlot(enemy,screen,4,2));
    }
    @Test void screenGuardsMustGetBetweenTheirArcherAndTheEnemy() {
        Vec3 enemy=new Vec3(0,80,0),archer=new Vec3(8,80,0);
        assertTrue(GoblinSquadTactics.inFrontOfArcher(enemy,new Vec3(2,80,0),archer));
        assertFalse(GoblinSquadTactics.inFrontOfArcher(enemy,new Vec3(-1,80,0),archer));
        assertFalse(GoblinSquadTactics.inFrontOfArcher(enemy,new Vec3(0,80,2),archer));
        assertFalse(GoblinSquadTactics.inFrontOfArcher(enemy,new Vec3(10,80,0),archer));
    }
    @Test void flankSlotsCoverTheCircleAndApproachAroundTheEnemy() {
        Vec3 enemy=new Vec3(0,80,0);
        var slots=new java.util.HashSet<Vec3>();
        for (int i=0;i<4;i++) {
            var slot=GoblinSquadTactics.ringSlot(enemy,i,4,0,1.05); slots.add(slot);
            assertEquals(1.05,slot.distanceTo(enemy),.00001);
        }
        assertEquals(4,slots.size());
        var step=GoblinSquadTactics.orbitWaypoint(new Vec3(3,80,0),enemy,new Vec3(-1.05,80,0),2.7);
        assertTrue(step.x>0 && step.z>0,"Take a tangent waypoint instead of charging through the enemy");
        assertEquals(2.7,step.distanceTo(enemy),.00001);
    }
    @Test void panickedRoutesCanBeLateralAndImperfectRatherThanAlwaysOptimal() {
        var position=new Vec3(6,80,0);var enemy=new Vec3(0,80,0);
        var lateral=GoblinSquadTactics.panicPoint(position,enemy,Math.PI/2,3);
        assertEquals(6,lateral.x,.0001);assertEquals(3,lateral.z,.0001);
        var wrongTurn=GoblinSquadTactics.panicPoint(position,enemy,Math.PI*2/3,3);
        assertTrue(wrongTurn.distanceTo(enemy)<position.distanceTo(enemy));
    }
    @Test void modestHighGroundIsPreferredOverAnOtherwiseEquivalentFiringSpot() {
        var ideal=new Vec3(8,80,0);
        assertTrue(GoblinSquadTactics.firingScore(ideal.add(0,1,0),ideal,ideal,80)
            <GoblinSquadTactics.firingScore(ideal,ideal,ideal,80));
    }
}
