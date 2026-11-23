package org.Azkerash.riftfall.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;

import yesman.epicfight.api.event.types.registry.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.mob.SkeletonPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.VindicatorPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch;

import java.util.Map;

/** Explicitly audited vanilla-derived humanoids; casters and custom bosses keep their own AI. */
public final class PlayerSystemCombat {
    public static final Map<String, String> HUMANOIDS =
            Map.of(
                    "orc_warrior", "vindicator",
                    "rotting_zombie", "zombie",
                    "gargoyle_striker", "zombie",
                    "decayed_skeleton", "skeleton",
                    "kobold_scout", "skeleton",
                    "death_knight", "wither_skeleton");

    public static EntityType<?> type(String name) {
        var id = ResourceLocation.fromNamespaceAndPath("player_system", name);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                ? BuiltInRegistries.ENTITY_TYPE.get(id)
                : null;
    }

    @SuppressWarnings("unchecked")
    public static void patches(EntityPatchRegistryEvent event) {
        HUMANOIDS.forEach(
                (name, preset) -> {
                    var type = type(name);
                    if (type == null) {
                        return;
                    }
                    Armatures.registerEntityTypeArmature(type, Armatures.BIPED);
                    event.registerEntityPatchUnsafe(
                            (EntityType<PathfinderMob>) type,
                            entity ->
                                    switch (preset) {
                                        case "vindicator" ->
                                                new VindicatorPatch<>((PathfinderMob) entity);
                                        case "wither_skeleton" ->
                                                new yesman.epicfight.world.capabilities.entitypatch
                                                        .mob.WitherSkeletonPatch<>(
                                                        (PathfinderMob) entity);
                                        case "zombie" -> new ZombiePatch<>((Zombie) entity);
                                        default -> new SkeletonPatch<>((AbstractSkeleton) entity);
                                    });
                });
    }

    private PlayerSystemCombat() {}
}
