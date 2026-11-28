package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.Mob;

import org.Azkerash.riftfall.compat.NativeOrcCast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import software.bernie.geckolib.event.GeoRenderEvent;

import yesman.epicfight.compat.geckolib.GeckolibCompat;

/** Allow the native spell renderer through Epic Fight's GeckoLib interception. */
@Mixin(value = GeckolibCompat.class, remap = false)
public abstract class NativeOrcGeckoRenderMixin {
    @Inject(method = "geoEntityRenderPreEvent", at = @At("HEAD"), cancellable = true)
    private void riftfall$nativeCast(GeoRenderEvent.Entity.Pre event, CallbackInfo ci) {
        if (event.getEntity() instanceof Mob mob && NativeOrcCast.active(mob)) {
            // Cancel this interception handler, leaving the GeckoLib render event uncanceled.
            ci.cancel();
        }
    }
}
