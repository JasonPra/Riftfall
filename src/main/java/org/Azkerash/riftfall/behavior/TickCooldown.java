package org.Azkerash.riftfall.behavior;

/** Absolute game-time cooldown; polling less frequently never lengthens its duration. */
public final class TickCooldown {
    private long readyAt = Long.MIN_VALUE;

    public boolean ready(long gameTime) {
        return gameTime >= readyAt;
    }

    public void start(long gameTime, int durationTicks) {
        if (durationTicks < 0) {
            throw new IllegalArgumentException("Negative cooldown");
        }
        readyAt = Math.addExact(gameTime, durationTicks);
    }
}
