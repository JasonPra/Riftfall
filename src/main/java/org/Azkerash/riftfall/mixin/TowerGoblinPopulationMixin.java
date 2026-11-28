package org.Azkerash.riftfall.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

import dev.kubas.playersystem.dungeon.*;
import dev.kubas.playersystem.dungeon.layout.BuildPlan;
import dev.kubas.playersystem.dungeon.motif.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import org.Azkerash.riftfall.compat.TowerGoblinPopulation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Predicate;

@Pseudo
@Mixin(targets = "dev.kubas.playersystem.dungeon.mob.DungeonSpawner", remap = false)
public abstract class TowerGoblinPopulationMixin {
    @WrapOperation(
            method = "populate",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ldev/kubas/playersystem/dungeon/motif/MobTable;roll(Lnet/minecraft/util/RandomSource;Ldev/kubas/playersystem/dungeon/DungeonRank;Ldev/kubas/playersystem/dungeon/motif/MobRole;Ljava/util/function/Predicate;)Ljava/util/Optional;"))
    private static Optional<SpawnEntry> riftfall$openingGoblinPack(
            MobTable table,
            RandomSource random,
            DungeonRank rank,
            MobRole role,
            Predicate<ResourceLocation> exists,
            Operation<Optional<SpawnEntry>> original,
            ServerLevel level,
            Instance instance,
            DungeonMotif motif,
            BuildPlan plan,
            int partySize,
            @Share("riftfall$goblinPack") LocalBooleanRef reserved) {
        var rolled = original.call(table, random, rank, role, exists);
        if (!reserved.get()
                && role == MobRole.WARRIOR
                && instance.theme().equals(DungeonDimensions.theme("tower"))) {
            var pack =
                    TowerGoblinPopulation.openingPack(table, rank)
                            .filter(entry -> exists.test(entry.entity()));
            if (pack.isPresent()) {
                reserved.set(true);
                return pack;
            }
        }
        return rolled;
    }
}
