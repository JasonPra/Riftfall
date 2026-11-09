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
    static AttackAnimation.Phase flurryPhase(float start,float windup,float contact,float recovery,float end) {
        return phase(start,windup,contact,recovery,end)
            .addProperty(AttackPhaseProperty.STUN_TYPE,StunType.SHORT)
            .addProperty(AttackPhaseProperty.IMPACT_MODIFIER,ValueModifier.setter(4))
            .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER,ValueModifier.multiplier(.65F));
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
    static net.minecraft.world.phys.Vec3 staffHead(GoblinEntity mob,float time) {
        var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,GoblinPatch.class);
        if (patch==null) return mob.position();
        var animation=GoblinAnimations.STAFF_SMASH.get();
        var matrix=patch.getArmature().getBoundTransformFor(animation.getPoseByTime(patch,time,1F),Armatures.BIPED.get().toolR);
        matrix.mulFront(patch.getModelMatrix(1F));
        var head=yesman.epicfight.api.utils.math.OpenMatrix4f.transform(matrix,new net.minecraft.world.phys.Vec3(.146,-.021875,-.962));
        return new net.minecraft.world.phys.Vec3(mob.getX()-head.x,mob.getY()+head.y,mob.getZ()-head.z);
    }
    static void groundImpact(GoblinEntity mob) {
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel world)) return;
        var from=staffHead(mob,.8F);
        var to=staffHead(mob,.95F).add(0,-.14*mob.getScale(),0);
        var hit=world.clip(new net.minecraft.world.level.ClipContext(from,to,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
        if (hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        var pos=hit.getLocation();
        var state=world.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        world.sendParticles(soulImpactParticle(),
            pos.x,pos.y+.06,pos.z,24,.3,.06,.3,.035);
        world.playSound(null,pos.x,pos.y,pos.z,net.minecraft.sounds.SoundEvents.WOOD_HIT,net.minecraft.sounds.SoundSource.HOSTILE,.7F,.7F);
        increment(mob,"RiftfallStaffGroundImpacts");
    }
    private static net.minecraft.core.particles.DustParticleOptions soulImpactParticle() {
        return new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.65F,.12F,.95F),1.15F);
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
            if (mob.level() instanceof net.minecraft.server.level.ServerLevel world) {
                world.sendParticles(soulImpactParticle(),target.getX(),target.getY()+target.getBbHeight()*.5,
                    target.getZ(),18,.22,.25,.22,.025);
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,target.getX(),
                    target.getY()+target.getBbHeight()*.5,target.getZ(),8,.2,.2,.2,.08);
            }
            increment(mob,"RiftfallClubHits");
        } else if (name.equals("riftfall:biped/goblin_grapple")) counter="RiftfallBackstabs";
        else if (name.equals("riftfall:biped/goblin_plunge")) counter="RiftfallPlungeHits";
        else if (name.equals("riftfall:biped/goblin_slash") || name.equals("riftfall:biped/goblin_backswing")
            || name.equals("riftfall:biped/goblin_flurry") || name.equals("riftfall:biped/goblin_twohand_flurry")) counter="RiftfallMeleeHits";
        else return;
        increment(mob,counter);
    }
    private static void increment(GoblinEntity mob,String key) {
        mob.getPersistentData().putInt(key,mob.getPersistentData().getInt(key)+1);
    }
    private GoblinWeaponAttacks() {}
}
