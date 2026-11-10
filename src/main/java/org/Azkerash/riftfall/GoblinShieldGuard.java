package org.Azkerash.riftfall;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.Azkerash.riftfall.shield.ShieldCollisions;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/** NPC adapter for Epic Fight's blocked-hit result, recoil, impact and attacker feedback. */
final class GoblinShieldGuard {
    static AttackResult defend(GoblinPatch patch,DamageSource source,AttackResult incoming) {
        var mob=patch.getOriginal();
        if(incoming.resultType!=AttackResult.ResultType.SUCCESS || incoming.damage<=0 || !mob.isBlocking()
                || source.getEntity()!=null && mob.isAlliedTo(source.getEntity()) || !ShieldCollisions.contact(mob,source))return incoming;
        if(!patch.isLogicalClient()) {
            mob.hurtCurrentlyUsedShield(incoming.damage);
            mob.playBlockedGrunt();
            patch.playSound(EpicFightSounds.CLASH.get(),-.05F,.1F);
            patch.playAnimationSynchronized(Animations.BIPED_HIT_SHIELD,0);
            if(source.getEntity()!=null) {
                var attacker=EpicFightCapabilities.getEntityPatch(source.getEntity(),LivingEntityPatch.class);
                if(attacker!=null)attacker.onAttackBlocked(source,patch);
                float impact=source instanceof EpicFightDamageSource epic?epic.calculateImpact():incoming.damage*.5F;
                patch.knockBackEntity(source.getEntity().position(),Math.min(.35F,.08F+Math.max(0,impact)*.04F));
                if(mob.level() instanceof ServerLevel level)
                    EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument(level,HitParticleType.FRONT_OF_EYES,HitParticleType.ZERO,mob,source.getEntity());
            }
        }
        return AttackResult.blocked(0);
    }
    private GoblinShieldGuard(){}
}
