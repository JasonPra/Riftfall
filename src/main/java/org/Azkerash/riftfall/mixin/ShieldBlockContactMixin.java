package org.Azkerash.riftfall.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.Azkerash.riftfall.shield.ShieldCollisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The ordinary shield path uses the same physical box when a shield item is in use. */
@Mixin(LivingEntity.class)
public abstract class ShieldBlockContactMixin {
    @Inject(method="isDamageSourceBlocked",at=@At("HEAD"),cancellable=true)
    private void riftfall$shieldContact(DamageSource source,CallbackInfoReturnable<Boolean> ci) {
        var entity=(LivingEntity)(Object)this;
        if(entity.isUsingItem() && ShieldCollisions.isShield(entity.getUseItem()))
            ci.setReturnValue(entity.isBlocking() && ShieldCollisions.contact(entity,source));
    }
}
