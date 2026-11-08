package org.Azkerash.riftfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RiftEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Riftfall.MODID);
    // Saved rift data is authoritative. Recreate the visual when its chunk loads rather
    // than saving a second copy of the portal's lifecycle in the entity chunk data.
    public static final DeferredHolder<EntityType<?>, EntityType<RiftEntity>> RIFT = TYPES.register("rift",
        () -> EntityType.Builder.<RiftEntity>of(RiftEntity::new, MobCategory.MISC)
            .sized(2.0F, 3.0F).clientTrackingRange(8).updateInterval(20)
            .setShouldReceiveVelocityUpdates(false).fireImmune().noSave().noSummon()
            .build("riftfall:rift"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoblinEntity>> GOBLIN = TYPES.register("goblin",
        () -> EntityType.Builder.of(GoblinEntity::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(8).build("riftfall:goblin"));
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinEntity>> GOBLIN_SHAMAN = TYPES.register("goblin_shaman",
        () -> EntityType.Builder.of(GoblinEntity::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(8).build("riftfall:goblin_shaman"));

    static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(GOBLIN.get(),GoblinEntity.attributes().build());
        event.put(GOBLIN_SHAMAN.get(),GoblinEntity.attributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,28).build());
    }
    static void patches(yesman.epicfight.api.event.types.registry.EntityPatchRegistryEvent event) {
        for (var type : java.util.List.of(GOBLIN.get(),GOBLIN_SHAMAN.get())) {
            yesman.epicfight.gameasset.Armatures.registerEntityTypeArmature(type,yesman.epicfight.gameasset.Armatures.BIPED);
            event.registerEntityPatchUnsafe(type,GoblinPatch::new);
        }
    }
    private RiftEntities() {}
}
