package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import org.Azkerash.riftfall.compat.EncounterGoblins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the original species and encounter setup, replacing only the entity factory result. */
@org.spongepowered.asm.mixin.Pseudo
@Mixin(
        targets = {
            "dev.kubas.playersystem.dungeon.mob.species.SpeciesApplier",
            "dev.kubas.playersystem.dungeon.mob.DungeonSpawner",
            "dev.kubas.quintessence.magic.world.Rifts"
        },
        remap = false)
public abstract class PlayerSystemEncounterSpawnMixin {
    @Redirect(
            method = "*",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"),
            require = 1)
    private static Entity riftfall$encounterBody(EntityType<?> type, Level level) {
        return EncounterGoblins.create(type, level);
    }
}
