package org.Azkerash.riftfall.client.wheel;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

import java.util.*;

class WheelSettingsTest {
    @Test
    void oldMagicBookEntryIsRemovedOnceAndItsSavedKeyCanBeRestored() {
        var settings = new WheelSettings();
        var magic = new WheelSettings.Group("Magic");
        magic.bindings.add("key.ars_nouveau.open_book");
        settings.wheels.add(magic);
        settings.wheelOnly.add("key.ars_nouveau.open_book");
        settings.originalKeys.put(
                "key.ars_nouveau.open_book", new WheelSettings.SavedKey("key.keyboard.c", "NONE"));
        settings.normalize();
        assertFalse(magic.bindings.contains("key.ars_nouveau.open_book"));
        assertFalse(settings.wheelOnly.contains("key.ars_nouveau.open_book"));
        assertEquals(
                "key.keyboard.c", settings.originalKeys.get("key.ars_nouveau.open_book").key());
        magic.bindings.add("key.ars_nouveau.open_book");
        settings.normalize();
        assertTrue(magic.bindings.contains("key.ars_nouveau.open_book"));
    }

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
