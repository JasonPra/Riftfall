package org.Azkerash.riftfall.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.kubas.playersystem.data.TitleView;
import dev.kubas.playersystem.panel.PanelActionHandlers;
import dev.kubas.spellatlas.SpellAtlas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import org.Azkerash.riftfall.compat.AtlasTitleBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(targets = "dev.kubas.playersystem.title.TitleService", remap = false)
public abstract class PlayerSystemAtlasTitlesMixin {
    @ModifyReturnValue(method = "view", at = @At("RETURN"))
    private static TitleView riftfall$atlasTitles(TitleView original, ServerPlayer player) {
        return AtlasTitleBridge.view(player, original);
    }

    @Inject(method = "selectById", at = @At("HEAD"), cancellable = true)
    private static void riftfall$selectAtlas(
            ServerPlayer player, String id, CallbackInfoReturnable<Boolean> ci) {
        if (AtlasTitleBridge.isBridge(id)) {
            ci.setReturnValue(AtlasTitleBridge.select(player, id));
        } else if (id != null
                && id.isEmpty()
                && !player.getData(SpellAtlas.DISCOVERED).title().isEmpty()
                && PanelActionHandlers.canAct(player)) {
            AtlasTitleBridge.nativeSelected(player);
            ci.setReturnValue(true);
        }
    }

    @Inject(method = "select", at = @At("RETURN"))
    private static void riftfall$nativeSelected(
            ServerPlayer player,
            Optional<ResourceLocation> id,
            CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ()) {
            AtlasTitleBridge.nativeSelected(player);
        }
    }
}
