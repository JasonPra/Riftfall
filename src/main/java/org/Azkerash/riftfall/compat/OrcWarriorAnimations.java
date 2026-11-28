package org.Azkerash.riftfall.compat;

import net.minecraft.world.entity.PathfinderMob;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.property.AnimationProperty.*;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.gameasset.Armatures.ArmatureAccessor;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/** Authored spell and living poses share the exact standard Epic Fight biped bind pose. */
public final class OrcWarriorAnimations {
    public static final ArmatureAccessor<HumanoidArmature> RIG =
            yesman.epicfight.gameasset.Armatures.BIPED;
    public static AnimationAccessor<StaticAnimation> IDLE, DEATH, AIR;
    public static AnimationAccessor<MovementAnimation> WALK;
    public static AnimationAccessor<ActionAnimation> SPELL_WINDUP, SPELL_SLAM;
    public static AnimationAccessor<HitAnimation> HURT;
    public static AnimationAccessor<LongHitAnimation> KNOCKDOWN;

    public static void register(AnimationBuilder builder) {
        IDLE =
                builder.nextAccessor(
                        "orc_warrior/idle", a -> new StaticAnimation(.15F, true, a, RIG));
        WALK =
                builder.nextAccessor(
                        "orc_warrior/walk", a -> new MovementAnimation(.15F, true, a, RIG));
        AIR = builder.nextAccessor("orc_warrior/air", a -> new StaticAnimation(.15F, true, a, RIG));
        DEATH =
                builder.nextAccessor(
                        "orc_warrior/death", a -> new StaticAnimation(.08F, false, a, RIG));
        HURT = builder.nextAccessor("orc_warrior/hurt", a -> new HitAnimation(.06F, a, RIG));
        KNOCKDOWN =
                builder.nextAccessor(
                        "orc_warrior/knockdown", a -> new LongHitAnimation(.08F, a, RIG));
        SPELL_WINDUP =
                builder.nextAccessor(
                        "orc_warrior/spell_windup",
                        a ->
                                new ActionAnimation(.08F, a, RIG)
                                        .addProperty(ActionAnimationProperty.STOP_MOVEMENT, false)
                                        .addProperty(
                                                ActionAnimationProperty.REMOVE_DELTA_MOVEMENT,
                                                false)
                                        .addProperty(
                                                StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, elapsed, partial) ->
                                                        speed
                                                                * 40F
                                                                / Math.max(
                                                                        1,
                                                                        patch.getOriginal()
                                                                                .getPersistentData()
                                                                                .getInt(
                                                                                        NativeOrcCast
                                                                                                .WINDUP))));
        SPELL_SLAM =
                builder.nextAccessor(
                        "orc_warrior/spell_slam",
                        a ->
                                new ActionAnimation(.05F, a, RIG)
                                        .addProperty(ActionAnimationProperty.STOP_MOVEMENT, false)
                                        .addProperty(
                                                ActionAnimationProperty.REMOVE_DELTA_MOVEMENT,
                                                false));
    }

    public static void living(Animator animator) {
        animator.addLivingAnimation(LivingMotions.IDLE, IDLE);
        animator.addLivingAnimation(LivingMotions.ANGRY, IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, WALK);
        animator.addLivingAnimation(LivingMotions.FALL, AIR);
        animator.addLivingAnimation(LivingMotions.JUMP, AIR);
        animator.addLivingAnimation(LivingMotions.LANDING_RECOVERY, HURT);
        animator.addLivingAnimation(LivingMotions.DEATH, DEATH);
    }

    public static CombatBehaviors.Builder<HumanoidMobPatch<?>> melee(PathfinderMob mob) {
        return yesman.epicfight.gameasset.MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS;
    }

    private OrcWarriorAnimations() {}
}
