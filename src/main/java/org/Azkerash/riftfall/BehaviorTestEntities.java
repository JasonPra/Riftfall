package org.Azkerash.riftfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.*;

/** Separate registry integration keeps the test actor out of the goblin implementation. */
public final class BehaviorTestEntities {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,Riftfall.MODID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Riftfall.MODID);
    public static final DeferredHolder<EntityType<?>,EntityType<BehaviorTestEntity>> TESTER=TYPES.register("behavior_tester",
        ()->EntityType.Builder.of(BehaviorTestEntity::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(8).build("riftfall:behavior_tester"));
    public static final DeferredItem<DeferredSpawnEggItem> EGG=ITEMS.register("behavior_tester_spawn_egg",
        ()->new DeferredSpawnEggItem(TESTER,0x663399,0x33DDCC,new Item.Properties()));
    static void register(IEventBus bus) {
        TYPES.register(bus);ITEMS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->e.put(TESTER.get(),BehaviorTestEntity.attributes().build()));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)-> {
            if (e.getTabKey()==CreativeModeTabs.SPAWN_EGGS) e.accept(EGG.get());
        });
    }
    private BehaviorTestEntities() {}
}
