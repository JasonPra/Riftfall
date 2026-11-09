package org.Azkerash.riftfall.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EndermanCarryVisualsTest {
    @Test void visibleBlockDisappearsOncePerCombatEntry() {
        var state = new EndermanCarryVisuals.State();
        assertFalse(state.tick(Vec3.ZERO, true, false));
        assertTrue(state.tick(Vec3.ZERO, true, true));
        assertFalse(state.tick(Vec3.ZERO, true, true));
        assertFalse(state.tick(Vec3.ZERO, true, false));
        assertTrue(state.tick(Vec3.ZERO, true, true));
    }

    @Test void arrivingInCombatOrNotHoldingABlockDoesNotPuff() {
        var state = new EndermanCarryVisuals.State();
        assertFalse(state.tick(Vec3.ZERO, true, true));
        state.tick(Vec3.ZERO, false, false);
        assertFalse(state.tick(Vec3.ZERO, false, true));
        state.tick(Vec3.ZERO, true, false);
        assertFalse(state.tick(Vec3.ZERO, false, true));
    }

    @Test void teleportBeforeCombatSuppressesThePuff() {
        var state = new EndermanCarryVisuals.State();
        state.tick(Vec3.ZERO, true, false);
        Vec3 destination = new Vec3(20, 0, 0);
        assertFalse(state.tick(destination, true, false));
        assertFalse(state.tick(destination, true, true));
        assertFalse(state.tick(destination, true, true));
    }

    @Test void teleportDuringCombatEntrySuppressesPuffButWalkingDoesNot() {
        var teleported = new EndermanCarryVisuals.State();
        teleported.tick(Vec3.ZERO, true, false);
        assertFalse(teleported.tick(new Vec3(20, 0, 0), true, true));
        var walking = new EndermanCarryVisuals.State();
        walking.tick(Vec3.ZERO, true, false);
        assertTrue(walking.tick(new Vec3(.2, 0, .1), true, true));
    }
}
