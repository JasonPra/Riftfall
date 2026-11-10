package org.Azkerash.riftfall;

import net.minecraft.world.entity.Mob;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class GoblinAnimations {
    public static AnimationAccessor<ActionAnimation> CHARGE, RELEASE, RECOVER, GRAPPLE_RELEASE, BURROW;
    public static AnimationAccessor<yesman.epicfight.api.animation.types.StaticAnimation> PANIC_RUN;
    public static AnimationAccessor<AttackAnimation> STAFF_SWEEP, STAFF_SMASH, SLASH, BACKSWING, FLURRY, SWORD_SWIPE, SWORD_REVERSE, FRENZY, SHIELD_OVERHEAD, PLUNGE, GRAPPLE;
    static void register(AnimationRegistryEvent event) {
        event.newBuilder(Riftfall.MODID,builder -> {
            PANIC_RUN=builder.nextAccessor("biped/goblin_panic_run",a->new yesman.epicfight.api.animation.types.StaticAnimation(.08F,true,a,Armatures.BIPED));
            BURROW=builder.nextAccessor("biped/goblin_burrow",a->new ActionAnimation(.08F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,true)
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                    (animation,patch,speed,elapsed,partial)->speed*50F/(patch.getOriginal() instanceof GoblinEntity goblin?goblin.burrowDuration():50)));
            GRAPPLE_RELEASE=builder.nextAccessor("biped/goblin_grapple_release",a->new ActionAnimation(.05F,a,Armatures.BIPED));
            GRAPPLE=builder.nextAccessor("biped/goblin_grapple",GoblinWeaponAttacks::grapple);
            BACKSWING=builder.nextAccessor("biped/goblin_backswing",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.30F,.48F,.75F,.9F)));
            FLURRY=builder.nextAccessor("biped/goblin_flurry",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.20F,.40F,.5F,.55F),
                GoblinWeaponAttacks.phase(.55F,.75F,.95F,1.05F,1.1F),
                GoblinWeaponAttacks.phase(1.1F,1.30F,1.50F,1.6F,1.7F)));
            SLASH=builder.nextAccessor("biped/goblin_slash",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.30F,.48F,.75F,.9F)));
            SWORD_SWIPE=builder.nextAccessor("biped/goblin_sword_swipe",a->GoblinWeaponAttacks.steppingAnimation(a,
                GoblinWeaponAttacks.phase(0,.17F,.32F,.4F,.6F)));
            SWORD_REVERSE=builder.nextAccessor("biped/goblin_sword_reverse",a->GoblinWeaponAttacks.steppingAnimation(a,
                GoblinWeaponAttacks.phase(0,.17F,.32F,.4F,.6F)));
            FRENZY=builder.nextAccessor("biped/goblin_frenzy",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.flurryPhase(0,.16F,.33F,.34F,.35F),
                GoblinWeaponAttacks.flurryPhase(.35F,.37F,.58F,.60F,.62F),
                GoblinWeaponAttacks.flurryPhase(.62F,.66F,.83F,.84F,.85F),
                GoblinWeaponAttacks.flurryPhase(.85F,.87F,1.08F,1.15F,1.4F))
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.COORD_SET_BEGIN,
                    yesman.epicfight.api.animation.property.MoveCoordFunctions.RAW_COORD)
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.COORD_SET_TICK,
                    yesman.epicfight.api.animation.property.MoveCoordFunctions.RAW_COORD));
            SHIELD_OVERHEAD=builder.nextAccessor("biped/goblin_shield_overhead",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.4F,.62F,.9F,1.2F)));
            PLUNGE=builder.nextAccessor("biped/goblin_plunge",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.70F,1.15F,1.4F,1.6F))
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.STOP_MOVEMENT,false)
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.REMOVE_DELTA_MOVEMENT,false));
            STAFF_SWEEP=builder.nextAccessor("biped/staff_sweep",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.6F,.78F,1.0F,1.5F)
                    .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER,
                        GoblinTactics.StaffAttack.SWEEP.damageModifier())));
            STAFF_SMASH=builder.nextAccessor("biped/staff_smash",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.8F,.98F,1.2F,1.9F)
                    .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER,
                        GoblinTactics.StaffAttack.SMASH.damageModifier())));
            CHARGE=builder.nextAccessor("biped/cast_charge",a->new ActionAnimation(.1F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            RELEASE=builder.nextAccessor("biped/cast_release",a->new ActionAnimation(.05F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            RECOVER=builder.nextAccessor("biped/cast_recover",a->new ActionAnimation(.08F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
        });
    }
    static void play(Mob mob,AnimationAccessor<? extends ActionAnimation> animation) {
        var patch=EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);
        if (patch!=null && animation!=null && !patch.isStunned()) patch.playAnimationSynchronized(animation,0);
    }
    private GoblinAnimations() {}
}
