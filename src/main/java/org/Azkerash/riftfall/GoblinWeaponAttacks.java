package org.Azkerash.riftfall;

import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.StunType;

/** Epic Fight owns swept weapon contact, damage delivery, and per-phase hit deduplication. */
final class GoblinWeaponAttacks {
    static AttackAnimation.Phase phase(float start,float windup,float contact,float recovery,float end) {
        return new AttackAnimation.Phase(start,windup,windup,contact,recovery,end,
            InteractionHand.MAIN_HAND,Armatures.BIPED.get().toolR,null)
            .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER,ValueModifier.setter(1));
    }
    static AttackAnimation animation(AnimationAccessor<? extends AttackAnimation> accessor,AttackAnimation.Phase... phases) {
        var animation=new AttackAnimation(.08F,accessor,Armatures.BIPED,phases);
        animation.addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR,0F);
        animation.addStateRemoveOld(EntityState.TURNING_LOCKED,false);
        return animation;
    }
    static AttackAnimation grapple(AnimationAccessor<? extends AttackAnimation> accessor) {
        var phases=new AttackAnimation.Phase[4];
        for (int i=0;i<4;i++) {
            float hit=1+i*.8F;
            phases[i]=phase(i==0 ? 0 : hit-.55F,hit-.10F,hit+.10F,hit+.25F,i==3 ? 4 : hit+.25F)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER,ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.STUN_TYPE,StunType.NONE);
        }
        return animation(accessor,phases);
    }
    @SubscribeEvent public static void hit(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getNewDamage()<=0
            || !(event.getSource() instanceof EpicFightDamageSource source)
            || !(source.getEntity() instanceof GoblinEntity mob) || source.getAnimation()==null) return;
        var animationName=source.getAnimation().registryName();
        if (animationName==null) return; // Epic Fight also wraps non-animation damage.
        String name=animationName.toString();
        String counter;
        if (name.equals("riftfall:biped/staff_sweep") || name.equals("riftfall:biped/staff_smash")) {
            boolean sweep=name.endsWith("staff_sweep");
            counter=sweep ? "RiftfallStaffSweepHits" : "RiftfallStaffSmashHits";
            var target=event.getEntity();
            target.knockback((sweep ? GoblinTactics.StaffAttack.SWEEP : GoblinTactics.StaffAttack.SMASH).knockback,mob.getX()-target.getX(),mob.getZ()-target.getZ());
            increment(mob,"RiftfallClubHits");
        } else if (name.equals("riftfall:biped/goblin_grapple")) counter="RiftfallBackstabs";
        else if (name.equals("riftfall:biped/goblin_plunge")) counter="RiftfallPlungeHits";
        else if (name.equals("riftfall:biped/goblin_slash") || name.equals("riftfall:biped/goblin_backswing")
            || name.equals("riftfall:biped/goblin_flurry")) counter="RiftfallMeleeHits";
        else return;
        increment(mob,counter);
    }
    private static void increment(GoblinEntity mob,String key) {
        mob.getPersistentData().putInt(key,mob.getPersistentData().getInt(key)+1);
    }
    private GoblinWeaponAttacks() {}
}
