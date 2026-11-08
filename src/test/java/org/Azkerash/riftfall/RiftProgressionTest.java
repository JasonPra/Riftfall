package org.Azkerash.riftfall;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftProgressionTest {
    @Test void playerDeathEssenceUnlocksLevelsWithinTheCap() {
        assertEquals(1, RiftProgression.level(0, 50, 10));
        assertEquals(1, RiftProgression.level(49, 50, 10));
        assertEquals(2, RiftProgression.level(50, 50, 10));
        assertEquals(4, RiftProgression.level(150, 50, 10));
        assertEquals(10, RiftProgression.level(Integer.MAX_VALUE, 50, 10));
    }

    @Test void EveryEncounterOverSixEnemiesHasAnExpandedBoundedArena() {
        assertEquals(6, RiftProgression.enemies(6, 1));
        assertEquals(10, RiftProgression.radius(6));
        for (int level = 2; level <= 10; level++) {
            int count = RiftProgression.enemies(6, level);
            assertTrue(count > 6);
            assertTrue(RiftProgression.radius(count) > 10);
            assertTrue(RiftProgression.radius(count) <= 26, "Neighboring arenas must stay separate");
        }
    }

    @Test void matureClustersRestoreCultivationGradually() {
        assertEquals(.05, CrystalCultivation.rate(.05, .05, 0), .000001);
        assertEquals(.10, CrystalCultivation.rate(.05, .05, 1), .000001);
        assertEquals(1.05, CrystalCultivation.rate(.05, .05, 20), .000001);
    }
}
