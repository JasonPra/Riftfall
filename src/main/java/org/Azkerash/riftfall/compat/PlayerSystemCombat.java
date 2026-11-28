package org.Azkerash.riftfall.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;

import yesman.epicfight.api.event.types.registry.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;

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
                    Armatures.registerEntityTypeArmature(
                            type,
                            name.equals("orc_warrior")
                                    ? OrcWarriorAnimations.RIG
                                    : Armatures.BIPED);
                    event.registerEntityPatchUnsafe(
                            (EntityType<PathfinderMob>) type,
                            entity ->
                                    switch (preset) {
                                        case "vindicator" ->
                                                new PlayerSystemAnimatedPatches.Vindicator(entity);
                                        case "wither_skeleton" ->
                                                new PlayerSystemAnimatedPatches.WitherSkeleton(
                                                        entity);
                                        case "zombie" ->
                                                new PlayerSystemAnimatedPatches.Zombie(entity);
                                        default -> new PlayerSystemAnimatedPatches.Skeleton(entity);
                                    });
                });
    }

    private PlayerSystemCombat() {}
}
