package org.Azkerash.riftfall.client.wheel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WheelLayoutTest {
    @Test
    void debugActionsAreRecognizedWithoutHidingOrdinaryUtilities() {
        assertTrue(
                WheelLayout.debug(
                        "key.riftfall.held_item_bounds", "key.categories.riftfall_debug"));
        assertTrue(
                WheelLayout.debug(
                        "key.epicfight.switch_vanilla_model_debug", "key.epicfight.system"));
        assertFalse(WheelLayout.debug("key.quintessence.thralls", "key.categories.quintessence"));
        assertFalse(WheelLayout.debug("key.epicfight.emote", "key.epicfight.gui"));
    }

    @Test
    void pointerSectorsMatchTheDrawnClockwiseWheelAndRespectDeadZone() {
        for (int count = 1; count <= 8; count++) {
            for (int index = 0; index < count; index++) {
                double angle = -Math.PI / 2 + index * Math.PI * 2 / count;
                assertEquals(
                        index,
                        WheelLayout.hovered(
                                Math.cos(angle) * 80, Math.sin(angle) * 80, 35, 120, count));
            }
        }
        assertEquals(-1, WheelLayout.hovered(0, 0, 35, 120, 8));
        assertEquals(-1, WheelLayout.hovered(121, 0, 35, 120, 8));
        assertEquals(-1, WheelLayout.hovered(80, 0, 35, 120, 0));
    }

    @Test
    void protectedPlayerSystemAndVanillaBindingsAreNeverEditable() {
        assertFalse(
                WheelLayout.eligible("key.player_system.skill_1", "key.categories.player_system"));
        assertFalse(WheelLayout.eligible("key.playersystem.panel", "key.categories.mod"));
        assertFalse(WheelLayout.eligible("key.forward", "key.categories.movement"));
        assertFalse(WheelLayout.eligible("key.riftfall.action_wheel", "key.categories.riftfall"));
        assertTrue(WheelLayout.eligible("key.quintessence.codex", "key.categories.quintessence"));
    }
}
