package org.Azkerash.riftfall.compat;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.*;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/** Keep native ranged/weapon support, specializing ordinary humanoid melee and locomotion. */
final class PlayerSystemAnimatedPatches {
    static final class Vindicator extends VindicatorPatch<PathfinderMob> {
        Vindicator(PathfinderMob mob) {
            super(mob);
        }

        @Override
        public void initAnimator(Animator animator) {
            super.initAnimator(animator);
            PlayerSystemAnimations.living(animator, original);
        }

        @Override
        protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
            return ordinaryWeapon(original)
                    ? PlayerSystemAnimations.melee(original)
                    : super.getHoldingItemWeaponMotionBuilder();
        }
    }

    static final class Zombie extends ZombiePatch<PathfinderMob> {
        Zombie(PathfinderMob mob) {
            super(mob);
        }

        @Override
        public void initAnimator(Animator animator) {
            super.initAnimator(animator);
            PlayerSystemAnimations.living(animator, original);
        }

        @Override
        protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
            return ordinaryWeapon(original)
                    ? PlayerSystemAnimations.melee(original)
                    : super.getHoldingItemWeaponMotionBuilder();
        }
    }

    static final class Skeleton extends SkeletonPatch<PathfinderMob> {
        Skeleton(PathfinderMob mob) {
            super(mob);
        }

        @Override
        public void initAnimator(Animator animator) {
            super.initAnimator(animator);
            PlayerSystemAnimations.living(animator, original);
        }

        @Override
        protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
            return ordinaryWeapon(original)
                    ? PlayerSystemAnimations.melee(original)
                    : super.getHoldingItemWeaponMotionBuilder();
        }
    }

    static final class WitherSkeleton extends WitherSkeletonPatch<PathfinderMob> {
        WitherSkeleton(PathfinderMob mob) {
            super(mob);
        }

        @Override
        public void initAnimator(Animator animator) {
            super.initAnimator(animator);
            PlayerSystemAnimations.living(animator, original);
        }

        @Override
        protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
            return ordinaryWeapon(original)
                    ? PlayerSystemAnimations.melee(original)
                    : super.getHoldingItemWeaponMotionBuilder();
        }
    }

    private static boolean ordinaryWeapon(PathfinderMob mob) {
        var item = mob.getMainHandItem();
        return item.isEmpty()
                || item.getItem() instanceof SwordItem
                || item.getItem() instanceof AxeItem;
    }

    private PlayerSystemAnimatedPatches() {}
}
