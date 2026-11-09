package org.Azkerash.riftfall.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.world.entity.monster.EnderMan;
import org.Azkerash.riftfall.client.EndermanCarryVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarriedBlockLayer.class)
public abstract class EndermanCarriedBlockLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/monster/EnderMan;FFFFFF)V",
        at = @At("HEAD"), cancellable = true)
    private void riftfall$hideBlockInCombat(PoseStack poses, MultiBufferSource buffers, int light, EnderMan enderman,
        float walk, float walkAmount, float partialTick, float age, float yaw, float pitch, CallbackInfo ci) {
        if (EndermanCarryVisuals.inCombat(enderman)) ci.cancel();
    }
}
