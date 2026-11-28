package org.Azkerash.riftfall.mixin;

import net.minecraft.server.level.ServerPlayer;

import org.Azkerash.riftfall.compat.AtlasTitleBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.kubas.spellatlas.ProgressEvents", remap = false)
public abstract class AtlasTitleSyncMixin {
    @Inject(method = "sendTitles", at = @At("TAIL"))
    private static void riftfall$refreshPanel(ServerPlayer player, CallbackInfo ci) {
        AtlasTitleBridge.atlasSynced(player);
    }
}
