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
    public static AnimationAccessor<ActionAnimation> CHARGE, RELEASE, RECOVER, GRAPPLE_RELEASE;
    public static AnimationAccessor<AttackAnimation> STAFF_SWEEP, STAFF_SMASH, SLASH, BACKSWING, FLURRY, PLUNGE, GRAPPLE;
    static void register(AnimationRegistryEvent event) {
        event.newBuilder(Riftfall.MODID,builder -> {
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
            PLUNGE=builder.nextAccessor("biped/goblin_plunge",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,.70F,1.15F,1.4F,1.6F))
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.STOP_MOVEMENT,false)
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.REMOVE_DELTA_MOVEMENT,false));
            STAFF_SWEEP=builder.nextAccessor("biped/staff_sweep",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,2.0F,2.2F,2.55F,2.8F)
                    .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER,
                        GoblinTactics.StaffAttack.SWEEP.damageModifier())));
            STAFF_SMASH=builder.nextAccessor("biped/staff_smash",a->GoblinWeaponAttacks.animation(a,
                GoblinWeaponAttacks.phase(0,2.05F,2.3F,2.7F,3.2F)
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
