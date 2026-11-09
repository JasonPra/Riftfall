package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.RenderEngine;
import org.Azkerash.riftfall.client.EndermanCarryVisuals;

/** Keep the carried block and the arms in the same coordinate system. */
@Mixin(value = RenderEngine.class, remap = false)
public abstract class EndermanCarryRenderMixin {
    @Inject(method = "epicfight$renderLivingPre", at = @At("HEAD"), cancellable = true)
    private void riftfall$vanillaCarryPose(RenderLivingEvent.Pre<?, ?> event, CallbackInfo ci) {
        if (event.getEntity() instanceof EnderMan enderman && enderman.getCarriedBlock() != null
            && !EndermanCarryVisuals.inCombat(enderman)) {
            // Skip only Epic Fight's render replacement. Do not cancel the NeoForge
            // event: vanilla must render both its carrying arms and its block layer.
            // The entity's Epic Fight AI and combat capabilities remain active.
            ci.cancel();
        }
    }
}
