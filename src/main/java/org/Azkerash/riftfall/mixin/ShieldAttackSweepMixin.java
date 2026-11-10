package org.Azkerash.riftfall.mixin;

import org.Azkerash.riftfall.shield.ShieldCollisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value=AttackAnimation.class,remap=false)
public abstract class ShieldAttackSweepMixin {
    @Inject(method="hurtCollidingEntities",at=@At("HEAD"))
    private void riftfall$begin(LivingEntityPatch<?> attacker,float previous,float current,EntityState before,EntityState after,AttackAnimation.Phase phase,CallbackInfo ci) {
        ShieldCollisions.beginSweep(attacker,(AttackAnimation)(Object)this,phase,previous,current);
    }
    @Inject(method="hurtCollidingEntities",at=@At("RETURN"))
    private void riftfall$end(LivingEntityPatch<?> attacker,float previous,float current,EntityState before,EntityState after,AttackAnimation.Phase phase,CallbackInfo ci){ShieldCollisions.endSweep();}
}
