package org.Azkerash.riftfall.client;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;

/** Both render paths must agree about whether the block is visible. */
public final class EndermanCarryVisuals {
    public static boolean inCombat(EnderMan enderman) {
        // DATA_CREEPY is synchronized even when vanilla's target is server-only.
        return enderman.isCreepy() || (enderman.getTarget() != null && enderman.getTarget().isAlive());
    }

    public static final class State {
        private Vec3 previousPosition;
        private boolean blockWasVisible;
        private int teleportGrace;

        public boolean tick(Vec3 position, boolean holding, boolean combat) {
            if (previousPosition != null && position.distanceToSqr(previousPosition) > 16) teleportGrace = 3;
            else if (teleportGrace > 0) teleportGrace--;
            boolean puff = blockWasVisible && holding && combat && teleportGrace == 0;
            blockWasVisible = holding && !combat;
            previousPosition = position;
            return puff;
        }
    }

    private EndermanCarryVisuals() {}
}
