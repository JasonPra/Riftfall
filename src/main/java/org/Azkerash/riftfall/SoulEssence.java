package org.Azkerash.riftfall;

import com.mojang.serialization.Codec;

/** Persisted with its chunk; elapsed unloaded or offline time is never added. */
record SoulEssence(double points) {
    static final SoulEssence EMPTY = new SoulEssence(0);
    static final Codec<SoulEssence> CODEC = Codec.doubleRange(0, Double.MAX_VALUE)
        .xmap(SoulEssence::new, SoulEssence::points);

    SoulEssence accumulate(double gain, double criticalMass) {
        return new SoulEssence(Math.min(criticalMass, points + gain));
    }

    boolean isCritical(double criticalMass) { return points >= criticalMass; }
}
