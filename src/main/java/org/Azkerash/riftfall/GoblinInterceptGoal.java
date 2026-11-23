package org.Azkerash.riftfall;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Reach a screen across the enemy's approach, then yield to ordinary melee at contact. */
final class GoblinInterceptGoal extends Goal {
    private final GoblinEntity mob;
    private long nextMove;

    GoblinInterceptGoal(GoblinEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        var plan = mob.squad.interception();
        return plan != null && mob.distanceToSqr(plan.position()) > .7 * .7 && !inMelee();
    }

    private boolean inMelee() {
        return mob.getTarget() != null
                && WeaponReach.canContact(mob, mob.getTarget(), GoblinAnimations.SWORD_SWIPE.get());
    }

    @Override
    public boolean canContinueToUse() {
        return !GoblinTactics.interrupted(mob) && canUse();
    }

    @Override
    public void start() {
        nextMove = 0;
        mob.getPersistentData().putBoolean("RiftfallIntercepting", true);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var plan = mob.squad.interception();
        if (plan == null) {
            return;
        }
        if (mob.level().getGameTime() >= nextMove) {
            nextMove = mob.level().getGameTime() + 5;
            mob.setSprinting(mob.getNavigation().moveTo(plan.path(), 1.25));
        }
        if (mob.getTarget() != null) {
            mob.getLookControl().setLookAt(mob.getTarget(), 75, 75);
        }
    }

    @Override
    public void stop() {
        mob.setSprinting(false);
        mob.getNavigation().stop();
        mob.getPersistentData().putBoolean("RiftfallIntercepting", false);
    }
}
