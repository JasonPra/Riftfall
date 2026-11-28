package org.Azkerash.riftfall.mixin;

import dev.kubas.spellatlas.Titles;

import org.Azkerash.riftfall.compat.AtlasTitleBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Panel labels accept a translation key with literal fallback; resolve Atlas's composite names. */
@Pseudo
@Mixin(targets = "dev.kubas.playersystem.title.TitleDefinition", remap = false)
public abstract class AtlasTitleNameMixin {
    @Inject(
            method = "nameKey(Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true)
    private static void riftfall$atlasName(String id, CallbackInfoReturnable<String> ci) {
        AtlasTitleBridge.decode(id)
                .ifPresent(title -> ci.setReturnValue(Titles.name(title).getString()));
    }
}
