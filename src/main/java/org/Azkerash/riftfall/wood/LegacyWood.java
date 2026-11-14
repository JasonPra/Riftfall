package org.Azkerash.riftfall.wood;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Building materials shared by the living Dynamic Trees Legacy family. */
public final class LegacyWood {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("riftfall");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("riftfall");
    private static BlockBehaviour.Properties properties(MapColor color){
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS)
            .strength(8.0F,1200.0F).requiresCorrectToolForDrops().lightLevel(state->3).sound(SoundType.WOOD).ignitedByLava();
    }
    public static final DeferredBlock<LegacyLogBlock> STRIPPED_LOG=BLOCKS.register("stripped_vermillion_legacy_log",()->new LegacyLogBlock(properties(MapColor.COLOR_ORANGE),null));
    public static final DeferredBlock<LegacyLogBlock> LOG=BLOCKS.register("vermillion_legacy_log",()->new LegacyLogBlock(properties(MapColor.PODZOL),STRIPPED_LOG));
    public static final DeferredBlock<net.minecraft.world.level.block.LeavesBlock> LEAVES=BLOCKS.register("vermillion_legacy_leaves",()->new net.minecraft.world.level.block.LeavesBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_LEAVES)));
    public static final DeferredBlock<HeartwoodFruitBlock> FRUIT_BLOCK=BLOCKS.register("heartwood_fruit",()->new HeartwoodFruitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(.2f).noCollission().noOcclusion().sound(SoundType.AZALEA_LEAVES).lightLevel(state->state.getValue(HeartwoodFruitBlock.STAGE)>0?2:0)));
    public static final DeferredItem<net.minecraft.world.item.Item> FRUIT=ITEMS.registerSimpleItem("heartwood_fruit",new net.minecraft.world.item.Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(.4f).alwaysEdible().effect(()->new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,200,0),1.0f).build()));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> LOG_ITEMS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("riftfall","vermillion_legacy_logs"));
    public static final DeferredItem<net.minecraft.world.item.Item> SAP_AMBER=ITEMS.registerSimpleItem("glinting_sap_amber",new net.minecraft.world.item.Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<net.minecraft.world.item.Item> SAP_BOTTLE=ITEMS.registerSimpleItem("heartwood_sap_bottle",new net.minecraft.world.item.Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));
    public static final DeferredItem<net.minecraft.world.item.Item> BARK=ITEMS.registerSimpleItem("vermillion_legacy_bark");
    static {ITEMS.registerSimpleBlockItem(LOG);ITEMS.registerSimpleBlockItem(STRIPPED_LOG);ITEMS.registerSimpleBlockItem(LEAVES);}
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);HeartwoodFruitEntity.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.NATURAL_BLOCKS){event.accept(LOG.get());event.accept(STRIPPED_LOG.get());event.accept(LEAVES.get());}
            if(event.getTabKey()==CreativeModeTabs.FOOD_AND_DRINKS)event.accept(FRUIT.get());
            if(event.getTabKey()==CreativeModeTabs.INGREDIENTS){event.accept(BARK.get());event.accept(SAP_AMBER.get());event.accept(SAP_BOTTLE.get());event.accept(FRUIT.get());}
        });
    }
    private LegacyWood(){}
}
