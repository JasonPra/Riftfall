package org.Azkerash.riftfall.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;

import org.Azkerash.riftfall.compat.EncounterGoblins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The original goblin eggs use the same replacement bodies as encounter spawns. */
@Mixin(SpawnEggItem.class)
public abstract class GoblinSpawnEggReplacementMixin {
    @ModifyReturnValue(method = "getType", at = @At("RETURN"))
    private EntityType<?> riftfall$goblinBody(EntityType<?> type) {
        return EncounterGoblins.replacement(type);
    }
}
