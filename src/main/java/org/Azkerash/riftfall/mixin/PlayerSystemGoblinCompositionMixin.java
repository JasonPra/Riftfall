package org.Azkerash.riftfall.mixin;

import dev.kubas.playersystem.dungeon.mob.species.*;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.compat.EncounterGoblinLoadout;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.kubas.playersystem.dungeon.mob.species.SpeciesApplier", remap = false)
public abstract class PlayerSystemGoblinCompositionMixin {
    @Redirect(
            method = "createBody",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
    private static Entity riftfall$groupBody(
            EntityType<?> type,
            Level creationLevel,
            ServerLevel level,
            MobSpecies species,
            double x,
            double y,
            double z,
            float yaw,
            MobSpawnType spawnType) {
        return EncounterGoblinLoadout.create(type, creationLevel, x, y, z);
    }

    @Inject(
            method =
                    "apply(Lnet/minecraft/world/entity/Mob;Ldev/kubas/playersystem/dungeon/mob/species/MobSpecies;Ldev/kubas/playersystem/dungeon/mob/species/MobRank;IZLnet/minecraft/util/RandomSource;DD)V",
            at = @At("TAIL"))
    private static void riftfall$restoreEquipment(
            Mob mob,
            MobSpecies species,
            MobRank rank,
            int tier,
            boolean elite,
            RandomSource random,
            double health,
            double damage,
            CallbackInfo callback) {
        org.Azkerash.riftfall.OrcEntity.restoreWeapon(mob);
        if (mob instanceof GoblinEntity goblin) {
            EncounterGoblinLoadout.restore(goblin);
        }
    }
}
