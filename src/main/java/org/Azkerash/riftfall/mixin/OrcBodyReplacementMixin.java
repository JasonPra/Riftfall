package org.Azkerash.riftfall.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import org.Azkerash.riftfall.OrcEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replace the body before any spawn setup or saved NBT is applied, retaining UUID and bookkeeping.
 */
@Mixin(EntityType.class)
public abstract class OrcBodyReplacementMixin {
    @Inject(
            method = "create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true)
    private void riftfall$orcBody(Level level, CallbackInfoReturnable<Entity> callback) {
        if (BuiltInRegistries.ENTITY_TYPE
                .getKey((EntityType<?>) (Object) this)
                .toString()
                .equals("player_system:orc_warrior")) {
            callback.setReturnValue(OrcEntities.ORC_WARRIOR.get().create(level));
        }
    }
}
