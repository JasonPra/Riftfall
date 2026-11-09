package org.Azkerash.riftfall.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.Azkerash.riftfall.client.HeldItemDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class HeldItemDebugMixin {
    @Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",shift=At.Shift.AFTER))
    private void riftfall$heldBounds(ItemStack stack,ItemDisplayContext context,boolean left,PoseStack poses,
        MultiBufferSource buffers,int light,int overlay,BakedModel model,CallbackInfo ci) {
        HeldItemDebug.draw(stack,context,poses,buffers,model);
    }
}
