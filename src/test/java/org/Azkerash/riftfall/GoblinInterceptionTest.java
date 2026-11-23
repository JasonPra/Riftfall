package org.Azkerash.riftfall;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;

import org.Azkerash.riftfall.behavior.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

class GoblinInterceptionTest {
    @Test
    void anticipatesApproachAndUsesSeparateBlockingLanes() {
        var threat = new Vec3(0, 80, 0);
        var ally = new Vec3(10, 80, 0);
        var still = GoblinSquadTactics.interceptPoint(threat, Vec3.ZERO, ally, 0);
        var moving = GoblinSquadTactics.interceptPoint(threat, new Vec3(.2, 0, 0), ally, 0);
        assertTrue(moving.x > still.x);
        assertTrue(moving.x < ally.x);
        var second = GoblinSquadTactics.interceptPoint(threat, Vec3.ZERO, ally, 1);
        assertNotEquals(still, second);
        assertTrue(GoblinSquadTactics.inFrontOfArcher(threat, second, ally));
    }

    @Test
    void dashAvoidsTinyGapsLongJumpsAndHeightChanges() {
        assertFalse(GoblinInterceptDash.dashGap(Vec3.ZERO, new Vec3(1, 0, 0)));
        assertTrue(GoblinInterceptDash.dashGap(Vec3.ZERO, new Vec3(4, 0, 0)));
        assertFalse(GoblinInterceptDash.dashGap(Vec3.ZERO, new Vec3(12, 0, 0)));
        assertFalse(GoblinInterceptDash.dashGap(Vec3.ZERO, new Vec3(4, 2, 0)));
    }

    @Test
    void interceptionOutranksPursuitButNotCommittedAttacksOrStun() {
        var engine = GoblinBehaviorProfile.engine();
        assertEquals(
                "intercept",
                engine.selectAction(
                        new BehaviorContext(Map.of("ready.intercept", 1D, "ready.melee", 1D)),
                        null));
        assertEquals(
                "melee",
                engine.selectAction(
                        new BehaviorContext(
                                Map.of(
                                        "ready.intercept",
                                        1D,
                                        "ready.melee",
                                        1D,
                                        "committed.melee",
                                        1D)),
                        "melee"));
        assertEquals(
                "interrupted",
                engine.selectAction(
                        new BehaviorContext(Map.of("ready.intercept", 1D, "self.interrupted", 1D)),
                        null));
    }

    @Test
    void rallySupportPreemptsOffenseButRespectsInterruptions() {
        var engine = GoblinBehaviorProfile.engine();
        assertEquals(
                "rally",
                engine.selectAction(
                        new BehaviorContext(
                                Map.of("ready.rally", 1D, "ready.cast", 1D, "ready.staff", 1D)),
                        null));
        assertEquals(
                "interrupted",
                engine.selectAction(
                        new BehaviorContext(Map.of("ready.rally", 1D, "self.interrupted", 1D)),
                        null));
    }
}
