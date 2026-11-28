package org.Azkerash.riftfall.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;

import org.Azkerash.riftfall.WeaponReach;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import java.util.HashMap;
import java.util.Map;

/** Species-specific locomotion and melee; the native orc telegraph retains its own damage. */
public final class PlayerSystemAnimations {
    private static final Map<String, AnimationAccessor<StaticAnimation>> IDLE = new HashMap<>();
    private static final Map<String, AnimationAccessor<MovementAnimation>> WALK = new HashMap<>();
    private static final Map<String, AnimationAccessor<AttackAnimation>> ATTACK = new HashMap<>();

    public static void register(AnimationBuilder builder) {
        OrcWarriorAnimations.register(builder);
        for (String name : PlayerSystemCombat.HUMANOIDS.keySet().stream().sorted().toList()) {
            if (name.equals("orc_warrior")) {
                continue;
            }
            IDLE.put(
                    name,
                    builder.nextAccessor(
                            "player_system/" + name + "_idle",
                            a -> new StaticAnimation(.15F, true, a, Armatures.BIPED)));
            WALK.put(
                    name,
                    builder.nextAccessor(
                            "player_system/" + name + "_walk",
                            a -> new MovementAnimation(.15F, true, a, Armatures.BIPED)));
            ATTACK.put(
                    name,
                    builder.nextAccessor(
                            "player_system/" + name + "_attack", a -> attack(name, a)));
        }
    }

    private static AttackAnimation attack(
            String name, AnimationAccessor<AttackAnimation> accessor) {
        float windup =
                name.equals("orc_warrior")
                        ? .5F
                        : name.equals("death_knight")
                                ? .45F
                                : name.equals("rotting_zombie") ? .35F : .25F;
        float contact = windup + .15F;
        float end =
                name.equals("orc_warrior")
                        ? 1.15F
                        : name.equals("death_knight")
                                ? 1.1F
                                : name.equals("rotting_zombie") ? .9F : .75F;
        var phase =
                new AttackAnimation.Phase(
                                0,
                                windup,
                                windup,
                                contact,
                                contact + .2F,
                                end,
                                InteractionHand.MAIN_HAND,
                                Armatures.BIPED.get().toolR,
                                null)
                        .addProperty(
                                AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1));
        return new AttackAnimation(.1F, accessor, Armatures.BIPED, phase)
                .addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0F);
    }

    public static void living(Animator animator, PathfinderMob mob) {
        if (NativeOrcCast.isOrc(mob)) {
            OrcWarriorAnimations.living(animator);
            return;
        }
        String name = name(mob);
        animator.addLivingAnimation(LivingMotions.IDLE, IDLE.get(name));
        animator.addLivingAnimation(LivingMotions.WALK, WALK.get(name));
        animator.addLivingAnimation(LivingMotions.CHASE, WALK.get(name));
    }

    public static CombatBehaviors.Builder<HumanoidMobPatch<?>> melee(PathfinderMob mob) {
        if (NativeOrcCast.isOrc(mob)) {
            return OrcWarriorAnimations.melee(mob);
        }
        var animation = ATTACK.get(name(mob));
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(
                        CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder()
                                .weight(1)
                                .cooldown(16)
                                .canBeInterrupted(false)
                                .nextBehavior(
                                        CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                                .withinDistance(0, 3)
                                                .custom(
                                                        p ->
                                                                !NativeOrcCast.active(
                                                                                p.getOriginal())
                                                                        && p.getOriginal()
                                                                                        .getTarget()
                                                                                != null
                                                                        && p.getOriginal()
                                                                                .hasLineOfSight(
                                                                                        p.getOriginal()
                                                                                                .getTarget())
                                                                        && WeaponReach.canContact(
                                                                                p.getOriginal(),
                                                                                p.getOriginal()
                                                                                        .getTarget(),
                                                                                animation.get()))
                                                .animationBehavior(animation)));
    }

    private static String name(PathfinderMob mob) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
    }

    private PlayerSystemAnimations() {}
}
