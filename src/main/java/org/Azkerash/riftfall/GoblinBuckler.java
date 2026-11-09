package org.Azkerash.riftfall;

import net.minecraft.world.InteractionHand;

/** Shield use runs alongside pursuit instead of stopping the warrior to guard. */
final class GoblinBuckler {
    static boolean shouldGuard(boolean equipped, boolean threat, boolean visible, boolean grounded,
                               boolean interrupted, boolean attacking, double distanceSquared) {
        return equipped && threat && visible && grounded && !interrupted && !attacking && distanceSquared <= 24 * 24;
    }

    static void lowerForAttack(GoblinEntity mob, int ticks) {
        mob.bucklerAttackUntil = mob.level().getGameTime() + ticks;
        mob.bucklerNextAttack = mob.bucklerAttackUntil + 20;
        if (mob.isUsingItem() && mob.getUsedItemHand() == InteractionHand.OFF_HAND) mob.stopUsingItem();
    }

    static boolean canAttack(GoblinEntity mob) {
        return !mob.getOffhandItem().is(GoblinItems.BUCKLER.get()) || mob.level().getGameTime() >= mob.bucklerNextAttack;
    }

    static void tick(GoblinEntity mob) {
        if (mob.isRanged()) return;
        var target = mob.getTarget();
        var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob, GoblinPatch.class);
        boolean attacking = mob.level().getGameTime() < mob.bucklerAttackUntil || mob.grappledPlayerId() >= 0
            || (patch != null && patch.getEntityState().inaction());
        boolean guard = shouldGuard(mob.getOffhandItem().is(GoblinItems.BUCKLER.get()),
            GoblinSquadTactics.validEnemy(mob, target), target != null && mob.getSensing().hasLineOfSight(target),
            mob.onGround() && !mob.isNoAi(), GoblinTactics.interrupted(mob), attacking,
            target == null ? Double.POSITIVE_INFINITY : mob.distanceToSqr(target));
        if (guard) {
            if (!mob.isUsingItem()) mob.startUsingItem(InteractionHand.OFF_HAND);
            mob.getLookControl().setLookAt(target, 90, 90);
        } else if (mob.isUsingItem() && mob.getUsedItemHand() == InteractionHand.OFF_HAND) mob.stopUsingItem();
        mob.getPersistentData().putBoolean("RiftfallBucklerRaised", mob.isUsingItem() && mob.getUsedItemHand() == InteractionHand.OFF_HAND);
    }
    private GoblinBuckler() {}
}
