package org.Azkerash.riftfall;

import net.minecraft.world.entity.Mob;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class RiftCastingAnimations {
    public static AnimationAccessor<ActionAnimation> CHARGE, RELEASE, RECOVER, CLUB, SLASH, BACKSWING, FLURRY, PLUNGE, GRAPPLE, GRAPPLE_RELEASE;
    static void register(AnimationRegistryEvent event) {
        event.newBuilder(Riftfall.MODID,builder -> {
            GRAPPLE_RELEASE=builder.nextAccessor("biped/goblin_grapple_release",a->new ActionAnimation(.05F,a,Armatures.BIPED));
            GRAPPLE=builder.nextAccessor("biped/goblin_grapple",a->new ActionAnimation(.08F,a,Armatures.BIPED));
            BACKSWING=builder.nextAccessor("biped/goblin_backswing",a->new ActionAnimation(.08F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            FLURRY=builder.nextAccessor("biped/goblin_flurry",a->new ActionAnimation(.08F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            SLASH=builder.nextAccessor("biped/goblin_slash",a->new ActionAnimation(.08F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            PLUNGE=builder.nextAccessor("biped/goblin_plunge",a->new ActionAnimation(.05F,a,Armatures.BIPED)
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.STOP_MOVEMENT,false)
                .addProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.REMOVE_DELTA_MOVEMENT,false));
            CLUB=builder.nextAccessor("biped/staff_club",a->new ActionAnimation(.08F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            CHARGE=builder.nextAccessor("biped/cast_charge",a->new ActionAnimation(.1F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            RELEASE=builder.nextAccessor("biped/cast_release",a->new ActionAnimation(.05F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
            RECOVER=builder.nextAccessor("biped/cast_recover",a->new ActionAnimation(.08F,a,Armatures.BIPED)
                .addStateRemoveOld(EntityState.TURNING_LOCKED,false));
        });
    }
    static void play(Mob mob,AnimationAccessor<ActionAnimation> animation) {
        var patch=EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);
        if (patch!=null && animation!=null && !patch.isStunned()) patch.playAnimationSynchronized(animation,0);
    }
    private RiftCastingAnimations() {}
}
