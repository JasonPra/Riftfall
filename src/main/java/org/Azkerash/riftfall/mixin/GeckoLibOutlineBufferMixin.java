package org.Azkerash.riftfall.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

/** GeckoLib 4.9.3: refreshing either half of an outlined entity consumer must not nest whole pairs. */
@Mixin(value = GeoRenderer.class, remap = false)
public interface GeckoLibOutlineBufferMixin {
    @Shadow
    private boolean bufferNeedsRefresh(VertexConsumer buffer) {
        throw new AssertionError("Shadow method");
    }

    @Inject(method = "checkAndRefreshBuffer", at = @At("HEAD"), cancellable = true)
    private void riftfall$refreshOutlinePair(boolean isReRender, VertexConsumer buffer,
            MultiBufferSource bufferSource, RenderType renderType,
            CallbackInfoReturnable<VertexConsumer> callback) {
        if (!isReRender && this instanceof GeoEntityRenderer<?> && bufferSource instanceof OutlineBufferSource
                && bufferNeedsRefresh(buffer)) {
            // getBuffer already returns the complete normal + outline consumer. Wrapping that result
            // into the old pair duplicates delivery to the same builders, producing zero-area quads.
            // Reacquire the entire consumer once. Healthy buffers, layer rerenders, armor and items
            // keep GeckoLib's original handling, including their foil/custom consumers.
            callback.setReturnValue(bufferSource.getBuffer(renderType));
        }
    }
}
