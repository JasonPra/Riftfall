package org.Azkerash.riftfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RiftBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Riftfall.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Riftfall.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Riftfall.MODID);
    public static final DeferredBlock<CrystalClusterBlock> CRYSTAL_CLUSTER = BLOCKS.register("crystal_cluster",
        () -> new CrystalClusterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
            .strength(1.5F).sound(SoundType.AMETHYST_CLUSTER).noOcclusion().lightLevel(state -> 12)));
    public static final DeferredItem<BlockItem> CRYSTAL_CLUSTER_ITEM = ITEMS.registerSimpleBlockItem(CRYSTAL_CLUSTER);
    public static final DeferredItem<SoulEssenceCrystalItem> SOUL_ESSENCE_CRYSTAL = ITEMS.register("soul_essence_crystal", SoulEssenceCrystalItem::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalClusterBlockEntity>> CRYSTAL_CLUSTER_ENTITY = ENTITIES.register("crystal_cluster",
        () -> BlockEntityType.Builder.of(CrystalClusterBlockEntity::new, CRYSTAL_CLUSTER.get()).build(null));

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(RiftBlocks::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) event.accept(CRYSTAL_CLUSTER_ITEM);
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(SOUL_ESSENCE_CRYSTAL);
    }

    private RiftBlocks() {}
}
