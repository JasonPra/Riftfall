package org.Azkerash.riftfall.compat;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

class PlayerSystemAnimationTest {
    @Test
    void clipsAreFiniteRigidTransformsAndLocomotionLoopsCloseCleanly() throws Exception {
        for (String mob : PlayerSystemCombat.HUMANOIDS.keySet()) {
            for (String kind : List.of("idle", "walk", "attack")) {
                String path =
                        mob.equals("orc_warrior")
                                ? "assets/riftfall/animmodels/animations/orc_warrior/"
                                        + kind
                                        + ".json"
                                : "assets/riftfall/animmodels/animations/player_system/"
                                        + mob
                                        + "_"
                                        + kind
                                        + ".json";
                try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
                    assertNotNull(stream, path);
                    var clip =
                            JsonParser.parseReader(
                                            new InputStreamReader(stream, StandardCharsets.UTF_8))
                                    .getAsJsonObject();
                    assertEquals(20, clip.getAsJsonArray("animation").size());
                    for (var value : clip.getAsJsonArray("animation")) {
                        var channel = value.getAsJsonObject();
                        var frames = channel.getAsJsonArray("transform");
                        var times = channel.getAsJsonArray("time");
                        assertEquals(frames.size(), times.size());
                        assertEquals(0, times.get(0).getAsFloat(), .0001);
                        for (int i = 0; i < frames.size(); i++) {
                            float[] data = new float[16];
                            var frame = frames.get(i).getAsJsonArray();
                            for (int k = 0; k < 16; k++) {
                                data[k] = frame.get(k).getAsFloat();
                                assertTrue(Float.isFinite(data[k]), path);
                            }
                            assertEquals(1, new Matrix4f().set(data).determinant(), .002, path);
                            if (i > 0) {
                                assertTrue(
                                        times.get(i).getAsFloat() > times.get(i - 1).getAsFloat(),
                                        path);
                            }
                        }
                        if (!kind.equals("attack")) {
                            assertEquals(
                                    frames.get(0),
                                    frames.get(frames.size() - 1),
                                    path + ": loop seam");
                        } else if (channel.get("name").getAsString().equals("Root")) {
                            for (var frame : frames) {
                                assertEquals(frames.get(0), frame, path + ": planted root");
                            }
                        }
                    }
                }
            }
        }
    }
}
