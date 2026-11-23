package org.Azkerash.riftfall.client;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;

import org.Azkerash.riftfall.compat.PlayerSystemCombat;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class EncounterHumanoidMeshTest {
    @Test
    void cubeFacesPointOutwardAfterCoordinateConversion() {
        var faces = new JsonObject();
        for (String name : new String[] {"north", "south", "east", "west", "up", "down"}) {
            faces.add(name, JsonParser.parseString("{\"uv\":[0,0],\"uv_size\":[2,2]}"));
        }
        var cube =
                JsonParser.parseString("{\"origin\":[-1,-1,-1],\"size\":[2,2,2]}")
                        .getAsJsonObject();
        cube.add("uv", faces);
        var bone =
                JsonParser.parseString("{\"name\":\"body\",\"pivot\":[0,0,0]}").getAsJsonObject();
        var cubes = new JsonArray();
        cubes.add(cube);
        bone.add("cubes", cubes);
        var geometry =
                JsonParser.parseString(
                                "{\"description\":{\"texture_width\":16,\"texture_height\":16}}")
                        .getAsJsonObject();
        var bones = new JsonArray();
        bones.add(bone);
        geometry.add("bones", bones);
        var geometries = new JsonArray();
        geometries.add(geometry);
        var source = new JsonObject();
        source.add("minecraft:geometry", geometries);
        var data = EncounterHumanoidMesh.convert(source).getAsJsonObject("vertices");
        var positions = data.getAsJsonObject("positions").getAsJsonArray("array");
        var normals = data.getAsJsonObject("normals").getAsJsonArray("array");
        for (int face = 0; face < 6; face++) {
            float dot = 0;
            for (int axis = 0; axis < 3; axis++) {
                dot +=
                        positions.get(face * 12 + axis).getAsFloat()
                                * normals.get(face * 3 + axis).getAsFloat();
            }
            assertTrue(dot > 0, "Face " + face + " must point away from cube center");
        }
    }

    @Test
    void installedHumanoidsProduceFiniteSkinnedMeshesAndValidTriangles() throws Exception {
        for (String name : PlayerSystemCombat.HUMANOIDS.keySet()) {
            String path = "assets/player_system/geo/mob/" + name + ".geo.json";
            try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                var source =
                        JsonParser.parseReader(
                                        new InputStreamReader(stream, StandardCharsets.UTF_8))
                                .getAsJsonObject();
                var data = EncounterHumanoidMesh.convert(source).getAsJsonObject("vertices");
                int vertexCount = data.getAsJsonObject("positions").get("count").getAsInt();
                int normalCount = data.getAsJsonObject("normals").get("count").getAsInt();
                assertTrue(vertexCount > 100, name);
                assertEquals(vertexCount, data.getAsJsonObject("uvs").get("count").getAsInt());
                assertEquals(vertexCount, data.getAsJsonObject("vcounts").get("count").getAsInt());
                for (String field : new String[] {"positions", "normals", "uvs"}) {
                    for (var value : data.getAsJsonObject(field).getAsJsonArray("array")) {
                        assertTrue(Float.isFinite(value.getAsFloat()), name + ": " + field);
                    }
                }
                var jointIndices = data.getAsJsonObject("vindices").getAsJsonArray("array");
                assertEquals(vertexCount * 2, jointIndices.size());
                for (int i = 0; i < jointIndices.size(); i += 2) {
                    assertTrue(
                            jointIndices.get(i).getAsInt() >= 0
                                    && jointIndices.get(i).getAsInt() < 20,
                            name);
                    assertEquals(0, jointIndices.get(i + 1).getAsInt());
                }
                for (var part : data.getAsJsonObject("parts").entrySet()) {
                    var indices = part.getValue().getAsJsonObject().getAsJsonArray("array");
                    assertEquals(0, indices.size() % 9, name);
                    for (int i = 0; i < indices.size(); i += 3) {
                        assertTrue(indices.get(i).getAsInt() < vertexCount, name);
                        assertTrue(indices.get(i + 1).getAsInt() < vertexCount, name);
                        assertTrue(indices.get(i + 2).getAsInt() < normalCount, name);
                    }
                }
            }
        }
    }
}
