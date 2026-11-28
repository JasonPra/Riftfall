package org.Azkerash.riftfall.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import org.Azkerash.riftfall.client.wheel.ActionWheelGuide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Pseudo
@Mixin(targets = "dev.kubas.quintessence.client.gui.codex.GuideTab", remap = false)
public abstract class QuintCodexControlsMixin {
    @Shadow private Map<String, ?> cache;

    @Inject(method = "blocks", at = @At("HEAD"))
    private void riftfall$liveControls(String topic, CallbackInfoReturnable<?> ci) {
        if (topic.equals("controls")) {
            cache.remove(topic);
        }
    }

    @Redirect(
            method = "build",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent riftfall$wheelReference(String key) {
        return key.equals("guide.quintessence.controls")
                ? ActionWheelGuide.controls()
                : Component.translatable(key);
    }
}
