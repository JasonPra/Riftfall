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

    private RiftEntities() {}
}
