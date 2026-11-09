package org.Azkerash.riftfall;

import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;

final class GoblinBuckler {
    /** Brief defensive pauses, leaving the warrior's ordinary attacks intact. */
    static final class Guard extends Goal {
        private final GoblinEntity mob;
        private long ready;
        private int remaining;
        Guard(GoblinEntity mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            return mob.level().getGameTime() >= ready && mob.getOffhandItem().is(GoblinItems.BUCKLER.get())
                && GoblinSquadTactics.validEnemy(mob, mob.getTarget()) && mob.onGround()
                && mob.distanceToSqr(mob.getTarget()) < 36 && !GoblinTactics.interrupted(mob)
                && mob.getSensing().hasLineOfSight(mob.getTarget()) && !busy();
        }
        private boolean busy() {
            var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob, GoblinPatch.class);
            return patch != null && patch.getEntityState().inaction();
        }
        @Override public void start() {
            remaining = 18;
            mob.getNavigation().stop(); mob.startUsingItem(InteractionHand.OFF_HAND);
        }
        @Override public boolean canContinueToUse() {
            return remaining > 0 && mob.isUsingItem() && mob.getOffhandItem().is(GoblinItems.BUCKLER.get())
                && GoblinSquadTactics.validEnemy(mob, mob.getTarget()) && !GoblinTactics.interrupted(mob);
        }
        @Override public void tick() {
            remaining--; mob.getLookControl().setLookAt(mob.getTarget(), 90, 90);
        }
        @Override public void stop() {
            mob.stopUsingItem(); ready = mob.level().getGameTime() + 70 + mob.getRandom().nextInt(40);
        }
    }
    private GoblinBuckler() {}
}
