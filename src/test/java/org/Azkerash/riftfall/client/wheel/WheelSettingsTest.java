package org.Azkerash.riftfall.client.wheel;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

import java.util.*;

class WheelSettingsTest {
    @Test
    void removingTheLastAssignmentReleasesItsWheelOnlyPolicyButKeepsRecoveryData() {
        var settings = new WheelSettings();
        var first = new WheelSettings.Group("Books");
        var second = new WheelSettings.Group("Magic");
        first.bindings.add("atlas");
        second.bindings.add("atlas");
        settings.wheels.addAll(List.of(first, second));
        settings.wheelOnly.add("atlas");
        settings.originalKeys.put("atlas", new WheelSettings.SavedKey("key.keyboard.g", "SHIFT"));
        first.bindings.clear();
        settings.normalize();
        assertTrue(settings.wheelOnly.contains("atlas"));
        second.bindings.clear();
        settings.normalize();
        assertFalse(settings.wheelOnly.contains("atlas"));
        assertEquals("SHIFT", settings.originalKeys.get("atlas").modifier());
        var restored = new Gson().fromJson(settings.json(), WheelSettings.class);
        restored.normalize();
        assertEquals(settings.originalKeys, restored.originalKeys);
    }

    @Test
    void incompleteConfigurationHasASafeEditableFallback() {
        var settings =
                new Gson()
                        .fromJson(
                                "{\"version\":1,\"wheels\":null,\"wheelOnly\":null,\"originalKeys\":null}",
                                WheelSettings.class);
        settings.normalize();
        assertEquals(1, settings.wheels.size());
        assertTrue(settings.wheelOnly.isEmpty());
    }
}
