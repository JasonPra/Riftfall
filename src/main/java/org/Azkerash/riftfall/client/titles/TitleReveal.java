package org.Azkerash.riftfall.client.titles;

/** Fast continuous magic sweep with a bounded, repeatable trail of rising stars. */
public final class TitleReveal {
    public static final int REVEAL_TICKS = 22, SPARKS = 64;
    public record Spark(float progress, float driftX, float driftY, float size, float opacity) {}

    public static float progress(float age) {
        float t = Math.clamp(age / REVEAL_TICKS, 0F, 1F);
        // Gentle ease in/out, with no character-count steps.
        return t * t * (3 - 2 * t);
    }
    public static Spark spark(int index, float age, int seed) {
        float birth = index * REVEAL_TICKS / (float)SPARKS;
        float life = 10 + random(index, seed, 1) * 13;
        float elapsed = age - birth;
        if (elapsed < 0 || elapsed >= life) return null;
        float t = elapsed / life;
        float a = Math.min(1F, elapsed / 2F) * (1 - t) * (1 - t);
        // Gentle twinkle, never fully blinking on/off between adjacent frames.
        a *= .65F + .35F * (float)Math.pow(Math.sin(elapsed * .5F + index), 2);
        float dx = (random(index, seed, 2) - .5F) * (4 + elapsed * .8F);
        float dy = (random(index, seed, 3) - .5F) * 20 - elapsed * (.35F + random(index, seed, 4) * .45F);
        return new Spark(progress(birth), dx, dy, (1.5F + random(index, seed, 5) * 2) * (1 - t * .6F), a);
    }
    private static float random(int index, int seed, int salt) {
        int h = seed ^ (index * 0x9E3779B9) ^ (salt * 0x85EBCA6B);
        h ^= h >>> 16; h *= 0x7FEB352D; h ^= h >>> 15; h *= 0x846CA68B; h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float)0x1000000;
    }
    private TitleReveal() {}
}
