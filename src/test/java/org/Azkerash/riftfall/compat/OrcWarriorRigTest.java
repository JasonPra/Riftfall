package org.Azkerash.riftfall.compat;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.*;

class OrcWarriorRigTest {
    private JsonObject asset(String path) {
        var stream =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("assets/riftfall/animmodels/" + path + ".json");
        assertNotNull(stream, path);
        return JsonParser.parseReader(
                        new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    @Test
    void authoredMeshHasIndependentLimbsAndToolAttachmentOnItsOwnRig() {
        var model = asset("entity/orc_warrior");
        var rig = model.getAsJsonObject("armature");
        assertEquals(20, rig.getAsJsonArray("joints").size());
        var vertices = model.getAsJsonObject("vertices");
        var parts = vertices.getAsJsonObject("parts");
        var weights = vertices.getAsJsonObject("vindices").getAsJsonArray("array");
        for (var part :
                Map.of(
                                "rightArm",
                                Set.of(11, 12),
                                "leftArm",
                                Set.of(16, 17),
                                "rightLeg",
                                Set.of(1, 2),
                                "leftLeg",
                                Set.of(4, 5),
                                "orcAxe",
                                Set.of(13))
                        .entrySet()) {
            Set<Integer> used = new HashSet<>();
            var indices = parts.getAsJsonObject(part.getKey()).getAsJsonArray("array");
            for (int i = 0; i < indices.size(); i += 3) {
                used.add(weights.get(indices.get(i).getAsInt() * 2).getAsInt());
            }
            assertTrue(used.containsAll(part.getValue()), part.getKey() + used);
        }
        for (int i = 0; i < weights.size(); i += 2) {
            assertTrue(weights.get(i).getAsInt() >= 0 && weights.get(i).getAsInt() < 20);
        }
    }

    @Test
    void allOrcClipsCoverTheDedicatedSkeletonAndDeathEndsInAFallenPose() {
        var expected = new HashSet<String>();
        for (var name :
                asset("entity/orc_warrior").getAsJsonObject("armature").getAsJsonArray("joints")) {
            expected.add(name.getAsString());
        }
        for (String clip :
                List.of(
                        "idle",
                        "walk",
                        "attack",
                        "spell_windup",
                        "spell_slam",
                        "hurt",
                        "knockdown",
                        "air",
                        "death")) {
            var channels = asset("animations/orc_warrior/" + clip).getAsJsonArray("animation");
            Set<String> actual = new HashSet<>();
            for (var element : channels) {
                var channel = element.getAsJsonObject();
                actual.add(channel.get("name").getAsString());
                var times = channel.getAsJsonArray("time");
                var frames = channel.getAsJsonArray("transform");
                assertEquals(times.size(), frames.size());
                for (int i = 0; i < times.size(); i++) {
                    if (i > 0) {
                        assertTrue(times.get(i).getAsFloat() > times.get(i - 1).getAsFloat());
                    }
                    for (var value : frames.get(i).getAsJsonArray()) {
                        assertTrue(Float.isFinite(value.getAsFloat()));
                    }
                }
                if (clip.equals("death") && channel.get("name").getAsString().equals("Root")) {
                    assertNotEquals(frames.get(0), frames.get(frames.size() - 1));
                    assertEquals(1.4F, times.get(times.size() - 1).getAsFloat(), .0001F);
                }
            }
            assertEquals(expected, actual, clip);
        }
    }
}
