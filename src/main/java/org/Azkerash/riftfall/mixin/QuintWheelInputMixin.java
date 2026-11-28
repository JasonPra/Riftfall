package org.Azkerash.riftfall.mixin;

import net.minecraft.client.KeyMapping;

import org.Azkerash.riftfall.client.wheel.WheelActions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A wheel opened from our menu stays open for click selection rather than needing its old key. */
@Pseudo
@Mixin(targets = "dev.kubas.quintessence.client.QKeys", remap = false)
public abstract class QuintWheelInputMixin {
    @Inject(method = "held", at = @At("HEAD"), cancellable = true)
    private static void riftfall$menuWheel(
            KeyMapping key, long window, CallbackInfoReturnable<Boolean> ci) {
        if (WheelActions.holdsQuintWheel(key)) {
            ci.setReturnValue(true);
        }
    }
}
