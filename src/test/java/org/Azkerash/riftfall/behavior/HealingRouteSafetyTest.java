package org.Azkerash.riftfall.behavior;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HealingRouteSafetyTest {
    @Test void rejectsEnemyCampingCircleOrStandingAcrossRoute() {
        var start=new Vec3(0,0,0);var circle=new Vec3(10,0,0);
        assertFalse(HealingRouteSafety.safe(start,List.of(circle),circle,List.of(circle),5,false));
        assertFalse(HealingRouteSafety.safe(start,List.of(circle),circle,List.of(new Vec3(5,0,0)),3,false));
    }
    @Test void safeDetourWorksAndConfidentActorCanRiskEnemyProximity() {
        var start=new Vec3(0,0,0);var circle=new Vec3(10,0,0);var enemy=new Vec3(5,0,0);
        assertTrue(HealingRouteSafety.safe(start,List.of(new Vec3(0,0,5),new Vec3(10,0,5),circle),circle,List.of(enemy),3,false));
        assertTrue(HealingRouteSafety.safe(start,List.of(circle),circle,List.of(enemy),3,true));
    }
    @Test void escapingImmediateDangerIsAllowedButReenteringItIsNot() {
        var start=new Vec3(2,0,0);var circle=new Vec3(10,0,0);var enemy=Vec3.ZERO;
        assertTrue(HealingRouteSafety.safe(start,List.of(circle),circle,List.of(enemy),5,false));
        assertFalse(HealingRouteSafety.safe(start,List.of(new Vec3(7,0,0),new Vec3(3,0,0),circle),circle,List.of(enemy),5,false));
    }
    @Test void samplesWholeSegmentsAndHandlesMultipleThreats() {
        var start=Vec3.ZERO;var circle=new Vec3(10,0,0);
        assertFalse(HealingRouteSafety.safe(start,List.of(circle),circle,List.of(new Vec3(20,0,20),new Vec3(5,0,0)),2,false));
        assertTrue(HealingRouteSafety.safe(start,List.of(circle),circle,List.of(),5,false));
    }
}
