package org.Azkerash.riftfall.confidence;

/** Per-observer tuning. Duration is captured when the death is witnessed; strength remains a profile factor. */
public record WitnessSettings(double radius,int durationTicks,int maxStacks) {
    public WitnessSettings {
        if (!Double.isFinite(radius) || radius<0 || radius>128 || durationTicks<0 || durationTicks>72000 || maxStacks<0 || maxStacks>32)
            throw new IllegalArgumentException("Invalid witnessed-death settings");
    }
    public static WitnessSettings defaults() { return new WitnessSettings(24,200,3); }
}
