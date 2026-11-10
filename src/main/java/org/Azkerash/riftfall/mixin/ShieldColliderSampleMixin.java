package org.Azkerash.riftfall.mixin;

import org.Azkerash.riftfall.shield.ShieldCollisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@Mixin(value=Collider.class,remap=false)
public abstract class ShieldColliderSampleMixin {
    @Inject(method="transform",at=@At("TAIL"))
    private void riftfall$sample(OpenMatrix4f matrix,CallbackInfo ci){ShieldCollisions.capture((Collider)(Object)this);}
}
