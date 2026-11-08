package org.Azkerash.riftfall;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class GoblinItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Riftfall.MODID);
    public static final net.neoforged.neoforge.registries.DeferredItem<Item> BONE_STAFF = ITEMS.registerSimpleItem("bone_staff",new Item.Properties().stacksTo(1).attributes(net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.WOOD,2,-2.8F)));
    private static final java.util.function.Supplier<Item> GOBLIN = ITEMS.register("goblin_spawn_egg",
        ()->new DeferredSpawnEggItem(RiftEntities.GOBLIN,0x507E39,0x5B4127,new Item.Properties()));
    private static final java.util.function.Supplier<Item> SHAMAN = ITEMS.register("goblin_shaman_spawn_egg",
        ()->new DeferredSpawnEggItem(RiftEntities.GOBLIN_SHAMAN,0x507E39,0x42285A,new Item.Properties()));
    private static final java.util.function.Supplier<Item> ARCHER = ITEMS.register("goblin_archer_spawn_egg",
        ()->new DeferredSpawnEggItem(RiftEntities.GOBLIN_ARCHER,0x507E39,0xA88653,new Item.Properties()));
    static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)-> {
            if (event.getTabKey()==CreativeModeTabs.COMBAT) event.accept(BONE_STAFF.get());
            if (event.getTabKey()==CreativeModeTabs.SPAWN_EGGS) { event.accept(GOBLIN.get()); event.accept(SHAMAN.get()); event.accept(ARCHER.get()); }
        });
    }
    private GoblinItems() {}
}
