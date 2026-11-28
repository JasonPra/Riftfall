package org.Azkerash.riftfall.mixin;

import dev.kubas.quintessence.magic.world.Rifts;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import org.Azkerash.riftfall.compat.EncounterGoblinLoadout;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "dev.kubas.quintessence.magic.world.Rifts", remap = false)
public abstract class QuintessenceGoblinCompositionMixin {
    @Redirect(
            method = "spawnKind",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
    private Entity riftfall$groupBody(
            EntityType<?> type,
            Level creationLevel,
            ServerLevel level,
            Rifts.Rift rift,
            EntityType<?> requested,
            double x,
            double z,
            int count,
            boolean guardian,
            ServerPlayer target) {
        return EncounterGoblinLoadout.create(type, creationLevel, x, rift.pos().getY(), z);
    }
}
