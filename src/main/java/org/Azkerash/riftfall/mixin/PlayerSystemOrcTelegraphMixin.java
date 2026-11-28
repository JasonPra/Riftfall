package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.Mob;

import org.Azkerash.riftfall.compat.NativeOrcCast;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "dev.kubas.playersystem.dungeon.mob.behavior.Telegraph", remap = false)
public abstract class PlayerSystemOrcTelegraphMixin {
    @Shadow private Mob mob;
    @Shadow @Final private int windupTicks;

    @Inject(method = "start", at = @At("RETURN"))
    private void riftfall$nativeWindup(Mob caster, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            NativeOrcCast.begin(caster, windupTicks);
        }
    }

    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/kubas/playersystem/TickGuard;run(Ljava/lang/String;Ljava/lang/Runnable;)Z"))
    private void riftfall$nativeRelease(long now, CallbackInfoReturnable<Boolean> callback) {
        NativeOrcCast.release(mob);
    }

    @Inject(method = "cancel()V", at = @At("HEAD"))
    private void riftfall$nativeCancel(CallbackInfo callback) {
        NativeOrcCast.cancel(mob);
    }
}
