package org.Azkerash.riftfall;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinBucklerTest {
    @Test void shieldsAreRaisedDuringTheApproachRatherThanOnlyAtMeleeDistance() {
        assertTrue(GoblinBuckler.shouldGuard(true, true, true, true, false, false, 16 * 16));
        assertTrue(GoblinBuckler.shouldGuard(true, true, true, true, false, false, 2 * 2));
        assertFalse(GoblinBuckler.shouldGuard(true, true, true, true, false, false, 25 * 25));
    }
    @Test void attacksAirborneMovementAndStunsLowerTheShield() {
        assertFalse(GoblinBuckler.shouldGuard(true, true, true, true, false, true, 4));
        assertFalse(GoblinBuckler.shouldGuard(true, true, true, false, false, false, 4));
        assertFalse(GoblinBuckler.shouldGuard(true, true, true, true, true, false, 4));
    }
    @Test void noShieldOrNoVisibleEnemyDoesNotTriggerGuarding() {
        assertFalse(GoblinBuckler.shouldGuard(false, true, true, true, false, false, 4));
        assertFalse(GoblinBuckler.shouldGuard(true, false, true, true, false, false, 4));
        assertFalse(GoblinBuckler.shouldGuard(true, true, false, true, false, false, 4));
    }
}
