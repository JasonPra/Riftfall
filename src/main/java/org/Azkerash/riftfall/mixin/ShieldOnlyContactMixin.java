package org.Azkerash.riftfall.mixin;

import net.minecraft.world.damagesource.DamageSource;
import org.Azkerash.riftfall.shield.ShieldCollisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value=LivingEntityPatch.class,remap=false)
public abstract class ShieldOnlyContactMixin {
    @Inject(method="tryHurt",at=@At("RETURN"),cancellable=true)
    private void riftfall$noPhantomBodyHit(DamageSource source,float amount,CallbackInfoReturnable<AttackResult> ci) {
        if(ci.getReturnValue().resultType==AttackResult.ResultType.SUCCESS && ShieldCollisions.shieldOnlyMiss(((LivingEntityPatch<?>)(Object)this).getOriginal(),source))
            ci.setReturnValue(AttackResult.missed(amount));
    }
}
