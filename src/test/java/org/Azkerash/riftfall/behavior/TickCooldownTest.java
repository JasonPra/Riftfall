package org.Azkerash.riftfall.behavior;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TickCooldownTest {
    @Test
    void cooldownExpiresAtTheSameGameTimeRegardlessOfPollingFrequency() {
        for (int poll : new int[] {1, 3, 10, 100}) {
            var cooldown = new TickCooldown();
            cooldown.start(100, 40);
            for (int t = 100; t < 140; t += poll) {
                assertFalse(cooldown.ready(t));
            }
            assertTrue(cooldown.ready(140));
            assertTrue(cooldown.ready(1000));
        }
    }

    @Test
    void restartUsesTheNewDeadlineAndZeroIsImmediatelyReady() {
        var cooldown = new TickCooldown();
        assertTrue(cooldown.ready(0));
        cooldown.start(5, 40);
        cooldown.start(10, 60);
        assertFalse(cooldown.ready(69));
        assertTrue(cooldown.ready(70));
        cooldown.start(70, 0);
        assertTrue(cooldown.ready(70));
    }

    @Test
    void invalidDurationsAndOverflowAreRejected() {
        var cooldown = new TickCooldown();
        assertThrows(IllegalArgumentException.class, () -> cooldown.start(0, -1));
        assertThrows(ArithmeticException.class, () -> cooldown.start(Long.MAX_VALUE, 1));
    }
}
