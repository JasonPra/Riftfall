package org.Azkerash.riftfall.client.titles;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArrivalTrackerTest {
    private final ArrivalTracker tracker = new ArrivalTracker();
    private String tick(String biome) { return tracker.tick(biome, 3, 8); }
    @Test void initialArrivalWaitsForDwellAndDoesNotRepeatWhileStaying() {
        assertNull(tick("plains")); assertNull(tick("plains")); assertEquals("plains", tick("plains"));
        for (int i = 0; i < 40; i++) assertNull(tick("plains"));
    }
    @Test void crossingBackAndForthDoesNotAnnounceTransientBiomes() {
        for (int i = 0; i < 20; i++) assertNull(tick(i % 2 == 0 ? "forest" : "plains"));
        assertNull(tick("forest")); assertNull(tick("forest")); assertEquals("forest", tick("forest"));
    }
    @Test void cooldownAnnouncesLatestBiomeRatherThanStaleQueue() {
        for (int i = 0; i < 3; i++) tick("plains");
        for (int i = 0; i < 3; i++) assertNull(tick("forest"));
        for (int i = 0; i < 4; i++) assertNull(tick("desert"));
        assertEquals("desert", tick("desert"));
    }
    @Test void revisitingBiomeAnnouncesAgainAfterCooldown() {
        for (int i = 0; i < 3; i++) tick("plains");
        for (int i = 0; i < 8; i++) tick("forest");
        for (int i = 0; i < 7; i++) assertNull(tick("plains"));
        assertEquals("plains", tick("plains"));
    }
    @Test void disconnectOrDimensionResetAllowsAnotherArrival() {
        for (int i = 0; i < 3; i++) tick("plains"); tracker.reset();
        assertNull(tick("plains")); assertNull(tick("plains")); assertEquals("plains", tick("plains"));
    }
    @Test void fadeIsContinuousWithTransparentEndpoints() {
        assertEquals(0, ArrivalTracker.opacity(0, 20, 60, 30));
        assertEquals(.5F, ArrivalTracker.opacity(10, 20, 60, 30));
        assertEquals(1, ArrivalTracker.opacity(20, 20, 60, 30));
        assertEquals(1, ArrivalTracker.opacity(80, 20, 60, 30));
        assertEquals(.5F, ArrivalTracker.opacity(95, 20, 60, 30));
        assertEquals(0, ArrivalTracker.opacity(110, 20, 60, 30));
        assertEquals(1, ArrivalTracker.opacity(0, 0, 60, 0));
        assertEquals(0, ArrivalTracker.opacity(60, 0, 60, 0));
    }
}
