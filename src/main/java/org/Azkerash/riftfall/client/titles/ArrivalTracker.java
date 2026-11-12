package org.Azkerash.riftfall.client.titles;

import java.util.Objects;

/** Debounces biome borders using client ticks rather than mutable world time. */
public final class ArrivalTracker {
    private String candidate, announced;
    private int stable, cooldown;

    public String tick(String biome, int dwell, int interval) {
        if (cooldown > 0) cooldown--;
        if (!Objects.equals(candidate, biome)) { candidate = biome; stable = 0; }
        stable = Math.min(stable + 1, Integer.MAX_VALUE - 1);
        if (stable >= dwell && cooldown == 0 && !Objects.equals(announced, biome)) {
            announced = biome;
            cooldown = interval;
            return biome;
        }
        return null;
    }
    public void reset() { candidate = announced = null; stable = cooldown = 0; }
    public static float opacity(float age, int fadeIn, int hold, int fadeOut) {
        if (age < 0 || age >= fadeIn + hold + fadeOut) return 0;
        if (fadeIn > 0 && age < fadeIn) return age / fadeIn;
        if (age < fadeIn + hold) return 1;
        return fadeOut == 0 ? 0 : (fadeIn + hold + fadeOut - age) / fadeOut;
    }
}
