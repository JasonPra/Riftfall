package org.Azkerash.riftfall.client;

import com.google.gson.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

/** Converts the installed pack's cube geometry, rather than redistributing another mod's models. */
public final class EncounterHumanoidMesh {
    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};
    private static final int[][] CORNERS = {
        {0, 1, 3, 2}, {5, 4, 6, 7}, {1, 5, 7, 3},
        {4, 0, 2, 6}, {2, 3, 7, 6}, {4, 5, 1, 0}
    };
    private static final Map<String, String> PARTS =
            Map.of(
                    "root",
                    "torso",
                    "body",
                    "torso",
                    "head",
                    "head",
                    "jaw",
                    "head",
                    "arm_r",
                    "rightArm",
                    "arm_l",
                    "leftArm",
                    "leg_r",
                    "rightLeg",
                    "leg_l",
                    "leftLeg");

    public static JsonObject convert(JsonObject source) {
        var geometry = source.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var description = geometry.getAsJsonObject("description");
        float width = description.get("texture_width").getAsFloat();
        float height = description.get("texture_height").getAsFloat();
        var positions = new JsonArray();
        var normals = new JsonArray();
        var uvs = new JsonArray();
        var counts = new JsonArray();
        var joints = new JsonArray();
        var parts = new LinkedHashMap<String, JsonArray>();
        for (String part :
                List.of(
                        "head",
                        "torso",
                        "rightArm",
                        "leftArm",
                        "rightLeg",
                        "leftLeg",
                        "hat",
                        "jacket",
                        "rightSleeve",
                        "leftSleeve",
                        "rightPants",
                        "leftPants")) {
            parts.put(part, new JsonArray());
        }
        Map<String, JsonObject> bones = new HashMap<>();
        for (var element : geometry.getAsJsonArray("bones")) {
            var bone = element.getAsJsonObject();
            bones.put(bone.get("name").getAsString(), bone);
        }
        for (var element : geometry.getAsJsonArray("bones")) {
            var bone = element.getAsJsonObject();
            String boneName = bone.get("name").getAsString();
            String part = part(boneName, bones);
            Matrix4f boneTransform = boneTransform(boneName, bones);
            var pivot = vector(bone.getAsJsonArray("pivot"));
            if (!bone.has("cubes")) {
                continue;
            }
            float bottom = Float.POSITIVE_INFINITY;
            for (var cube : bone.getAsJsonArray("cubes")) {
                bottom =
                        Math.min(
                                bottom,
                                cube.getAsJsonObject()
                                        .getAsJsonArray("origin")
                                        .get(1)
                                        .getAsFloat());
            }
            float split = (pivot.y + bottom) * .5F;
            for (var elementCube : bone.getAsJsonArray("cubes")) {
                var cube = elementCube.getAsJsonObject();
                var origin = vector(cube.getAsJsonArray("origin"));
                var size = vector(cube.getAsJsonArray("size"));
                float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0;
                var transform = new Matrix4f(boneTransform).mul(rotation(cube));
                var vertices = new Vector3f[8];
                for (int i = 0; i < 8; i++) {
                    vertices[i] =
                            new Vector3f(
                                    origin.x + ((i & 1) != 0 ? size.x + inflate : -inflate),
                                    origin.y + ((i & 2) != 0 ? size.y + inflate : -inflate),
                                    origin.z + ((i & 4) != 0 ? size.z + inflate : -inflate));
                    transform.transformPosition(vertices[i]);
                }
                if (!cube.get("uv").isJsonObject()) {
                    throw new IllegalArgumentException(
                            "Encounter model requires per-face UVs: " + boneName);
                }
                var faceUvs = cube.getAsJsonObject("uv");
                for (int face = 0; face < 6; face++) {
                    if (!faceUvs.has(FACES[face])) {
                        continue;
                    }
                    var uv = faceUvs.getAsJsonObject(FACES[face]);
                    var start = uv.getAsJsonArray("uv");
                    var extent = uv.getAsJsonArray("uv_size");
                    float u = start.get(0).getAsFloat() / width,
                            v = start.get(1).getAsFloat() / height;
                    float du = extent.get(0).getAsFloat() / width,
                            dv = extent.get(1).getAsFloat() / height;
                    float[][] tex =
                            face < 4
                                    ? new float[][] {
                                        {u, v + dv}, {u + du, v + dv}, {u + du, v}, {u, v}
                                    }
                                    : new float[][] {
                                        {u, v}, {u + du, v}, {u + du, v + dv}, {u, v + dv}
                                    };
                    int base = positions.size() / 3;
                    var converted = new Vector3f[4];
                    for (int i = 0; i < 4; i++) {
                        var vertex = vertices[CORNERS[face][i]];
                        converted[i] = new Vector3f(-vertex.x, -vertex.z, vertex.y).div(16);
                        add(positions, converted[i].x, converted[i].y, converted[i].z);
                        add(uvs, tex[i][0], tex[i][1]);
                        counts.add(1);
                        joints.add(joint(part, vertex.y, split));
                        joints.add(0);
                    }
                    // Corner order is inward in source coordinates; reflection makes it outward.
                    var normal =
                            new Vector3f(converted[1])
                                    .sub(converted[0])
                                    .cross(new Vector3f(converted[2]).sub(converted[0]))
                                    .normalize();
                    int normalIndex = normals.size() / 3;
                    add(normals, normal.x, normal.y, normal.z);
                    for (int index : new int[] {0, 1, 2, 0, 2, 3}) {
                        parts.get(part).add(base + index);
                        parts.get(part).add(base + index);
                        parts.get(part).add(normalIndex);
                    }
                }
            }
        }
        var data = new JsonObject();
        data.add("positions", buffer(positions, 3));
        data.add("normals", buffer(normals, 3));
        data.add("uvs", buffer(uvs, 2));
        var weights = new JsonArray();
        weights.add(1F);
        data.add("weights", buffer(weights, 1));
        data.add("vcounts", buffer(counts, 1));
        data.add("vindices", buffer(joints, 1));
        var groups = new JsonObject();
        parts.forEach((name, indices) -> groups.add(name, buffer(indices, 3)));
        data.add("parts", groups);
        var result = new JsonObject();
        result.add("vertices", data);
        return result;
    }

    private static int joint(String part, float y, float split) {
        return switch (part) {
            case "head" -> 9;
            case "rightArm" -> y < split ? 12 : 11;
            case "leftArm" -> y < split ? 17 : 16;
            case "rightLeg" -> y < split ? 2 : 1;
            case "leftLeg" -> y < split ? 5 : 4;
            default -> y < split ? 7 : 8;
        };
    }

    private static String part(String name, Map<String, JsonObject> bones) {
        if (PARTS.containsKey(name)) {
            return PARTS.get(name);
        }
        var bone = bones.get(name);
        return bone != null && bone.has("parent")
                ? part(bone.get("parent").getAsString(), bones)
                : "torso";
    }

    private static Matrix4f boneTransform(String name, Map<String, JsonObject> bones) {
        var bone = bones.get(name);
        var parent =
                bone.has("parent")
                        ? boneTransform(bone.get("parent").getAsString(), bones)
                        : new Matrix4f();
        return parent.mul(rotation(bone));
    }

    private static Matrix4f rotation(JsonObject element) {
        if (!element.has("rotation")) {
            return new Matrix4f();
        }
        var pivot = element.has("pivot") ? vector(element.getAsJsonArray("pivot")) : new Vector3f();
        var angles = vector(element.getAsJsonArray("rotation")).mul((float) Math.PI / 180);
        return new Matrix4f()
                .translation(pivot)
                .rotateZYX(angles.z, angles.y, angles.x)
                .translate(new Vector3f(pivot).negate());
    }

    private static Vector3f vector(JsonArray a) {
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private static void add(JsonArray array, float... values) {
        for (float value : values) {
            array.add(value);
        }
    }

    private static JsonObject buffer(JsonArray values, int stride) {
        var result = new JsonObject();
        result.addProperty("stride", stride);
        result.addProperty("count", values.size() / stride);
        result.add("array", values);
        return result;
    }

    private EncounterHumanoidMesh() {}
}
