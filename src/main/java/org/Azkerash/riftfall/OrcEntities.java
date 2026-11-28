package org.Azkerash.riftfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OrcEntities {
    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Riftfall.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<OrcEntity>> ORC_WARRIOR =
            TYPES.register(
                    "orc_warrior",
                    () ->
                            EntityType.Builder.of(OrcEntity::new, MobCategory.MONSTER)
                                    .sized(.8F, 2.25F)
                                    .clientTrackingRange(10)
                                    .build("riftfall:orc_warrior"));

    private OrcEntities() {}
}
