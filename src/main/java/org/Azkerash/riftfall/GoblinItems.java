package org.Azkerash.riftfall;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class GoblinItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Riftfall.MODID);
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.SwordItem> SHORTSWORD = ITEMS.register("goblin_shortsword",
        () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.STONE,
            new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.STONE,3,-2.4F))));
    public static final net.neoforged.neoforge.registries.DeferredItem<Item> BONE_STAFF = ITEMS.registerSimpleItem("bone_staff",new Item.Properties().stacksTo(1).attributes(net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.WOOD,2,-2.8F)));
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.ShieldItem> BUCKLER = ITEMS.register("goblin_buckler", () -> new net.minecraft.world.item.ShieldItem(new Item.Properties().durability(180)));
    private static final java.util.function.Supplier<Item> GOBLIN = ITEMS.register("goblin_spawn_egg",
        ()->new DeferredSpawnEggItem(GoblinEntities.GOBLIN,0x507E39,0x5B4127,new Item.Properties()));
    private static final java.util.function.Supplier<Item> SHAMAN = ITEMS.register("goblin_shaman_spawn_egg",
        ()->new DeferredSpawnEggItem(GoblinEntities.GOBLIN_SHAMAN,0x507E39,0x42285A,new Item.Properties()));
    private static final java.util.function.Supplier<Item> ARCHER = ITEMS.register("goblin_archer_spawn_egg",
        ()->new DeferredSpawnEggItem(GoblinEntities.GOBLIN_ARCHER,0x507E39,0xA88653,new Item.Properties()));
    static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)-> {
            if (event.getTabKey()==CreativeModeTabs.COMBAT) { event.accept(SHORTSWORD.get()); event.accept(BONE_STAFF.get()); event.accept(BUCKLER.get()); }
            if (event.getTabKey()==CreativeModeTabs.SPAWN_EGGS) { event.accept(GOBLIN.get()); event.accept(SHAMAN.get()); event.accept(ARCHER.get()); }
        });
    }
    private GoblinItems() {}
}
