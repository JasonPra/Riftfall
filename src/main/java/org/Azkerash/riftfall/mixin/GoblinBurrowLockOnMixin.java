package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.Azkerash.riftfall.GoblinEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

/** Release the camera and its focus/highlight together, and reject automatic reacquisition. */
@Mixin(value=EpicFightCameraAPI.class,remap=false)
public abstract class GoblinBurrowLockOnMixin {
    // Epic Fight exposes setLockOn, but clearing focus and synchronizing the target are private.
    @Invoker("setFocusingEntity") protected abstract void riftfall$setFocus(LivingEntity target);
    @Invoker("sendTargeting") protected abstract void riftfall$sendTarget(LivingEntity target);
    @ModifyVariable(method="setFocusingEntity",at=@At("HEAD"),argsOnly=true)
    private LivingEntity riftfall$filterBurrowingTarget(LivingEntity target) {
        return target instanceof GoblinEntity goblin && goblin.isBurrowLockHidden()?null:target;
    }
    @Inject(method="postClientTick",at=@At("TAIL"))
    private void riftfall$releaseBurrowingTarget(CallbackInfo ci) {
        var camera=(EpicFightCameraAPI)(Object)this;
        if(camera.getFocusingEntity() instanceof GoblinEntity goblin && goblin.isBurrowLockHidden()) {
            camera.setLockOn(false);
            riftfall$setFocus(null);
            riftfall$sendTarget(null);
        }
    }
}
