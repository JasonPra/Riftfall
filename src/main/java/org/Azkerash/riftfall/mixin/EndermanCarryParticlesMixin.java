package org.Azkerash.riftfall.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.monster.EnderMan;
import org.Azkerash.riftfall.client.EndermanCarryVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-only transition effect; never changes the actual carried block. */
@Mixin(EnderMan.class)
public abstract class EndermanCarryParticlesMixin {
    @Unique private final EndermanCarryVisuals.State riftfall$carryState = new EndermanCarryVisuals.State();

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void riftfall$hideBlockPuff(CallbackInfo ci) {
        EnderMan enderman = (EnderMan) (Object) this;
        if (riftfall$carryState.tick(enderman.position(), enderman.getCarriedBlock() != null,
            EndermanCarryVisuals.inCombat(enderman))) {
            double yaw = Math.toRadians(enderman.yBodyRot);
            double x = enderman.getX() - Math.sin(yaw) * .55;
            double z = enderman.getZ() + Math.cos(yaw) * .55;
            var random = enderman.getRandom();
            for (int i = 0; i < 28; i++) enderman.level().addParticle(ParticleTypes.PORTAL,
                x + (random.nextDouble() - .5) * .65, enderman.getY() + .7 + random.nextDouble() * .5,
                z + (random.nextDouble() - .5) * .65,
                (random.nextDouble() - .5) * .5, random.nextDouble() * .3, (random.nextDouble() - .5) * .5);
        }
    }
}
