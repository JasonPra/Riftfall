package org.Azkerash.riftfall.client.wheel;

import com.google.gson.GsonBuilder;

import java.util.*;

/**
 * Client-local preferences. Original bindings are retained for reversible wheel-only assignment.
 */
public final class WheelSettings {
    public int version = 1;
    public List<Group> wheels = new ArrayList<>();
    public Set<String> wheelOnly = new LinkedHashSet<>();
    public Map<String, SavedKey> originalKeys = new LinkedHashMap<>();

    public record SavedKey(String key, String modifier) {}

    public static final class Group {
        public String name;
        public List<String> bindings = new ArrayList<>();

        public Group(String name) {
            this.name = name;
        }
    }

    public void normalize() {
        if (wheels == null) {
            wheels = new ArrayList<>();
        }
        wheels.removeIf(g -> g == null || g.name == null || g.name.isBlank());
        for (var group : wheels) {
            if (group.bindings == null) {
                group.bindings = new ArrayList<>();
            }
            group.bindings =
                    new ArrayList<>(
                            new LinkedHashSet<>(
                                    group.bindings.stream().filter(Objects::nonNull).toList()));
        }
        if (wheelOnly == null) {
            wheelOnly = new LinkedHashSet<>();
        }
        if (originalKeys == null) {
            originalKeys = new LinkedHashMap<>();
        }
        wheelOnly.retainAll(wheels.stream().flatMap(g -> g.bindings.stream()).toList());
        if (wheels.isEmpty()) {
            wheels.add(new Group("Actions"));
        }
    }

    public String json() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
