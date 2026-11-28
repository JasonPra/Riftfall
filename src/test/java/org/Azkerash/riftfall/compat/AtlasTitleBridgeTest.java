package org.Azkerash.riftfall.compat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class AtlasTitleBridgeTest {
    @Test
    void atlasIdsRoundTripWithoutCollidingWithNativeTitles() {
        for (String title :
                List.of(
                        "nemesis:1",
                        "nemesis:5",
                        "school:irons_spellbooks:fire",
                        "beast:dim:minecraft:the_nether",
                        "grandmaster")) {
            var encoded = AtlasTitleBridge.encode(title);
            assertEquals(title, AtlasTitleBridge.decode(encoded).orElseThrow());
            assertTrue(encoded.length() <= 128);
        }
        assertNotEquals(AtlasTitleBridge.encode("nemesis:1"), AtlasTitleBridge.encode("nemesis_1"));
        assertTrue(AtlasTitleBridge.decode("player_system:elite").isEmpty());
    }

    @Test
    void malformedAndOversizedWireIdsAreRejected() {
        for (String id :
                List.of(
                        "riftfall:atlas/",
                        "riftfall:atlas/zz",
                        "riftfall:atlas/1",
                        "riftfall:atlas/ff",
                        "riftfall:atlas/" + "00".repeat(128))) {
            assertTrue(AtlasTitleBridge.decode(id).isEmpty(), id);
        }
    }
}
