package org.Azkerash.riftfall.flora;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Manaheart ground cover uses normal biome placed features and ordinary block items. */
public final class ManaheartFlora {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("riftfall");
    private static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(net.minecraft.core.registries.Registries.FEATURE,"riftfall");
    static {FEATURES.register("starweave_boulder",StarweaveBoulderFeature::new);}
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("riftfall");
    public static final DeferredBlock<StarweaveMossBlock> MOSS=BLOCKS.register("starweave_moss",()->new StarweaveMossBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK)));
    public static final DeferredBlock<CarpetBlock> CARPET=BLOCKS.register("starweave_moss_carpet",()->new CarpetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET)));
    public static final DeferredBlock<Block> BOULDER=BLOCKS.register("starweave_boulderstone",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_COBBLESTONE)));
    public static final DeferredBlock<WhisperfrondBlock> FERN=BLOCKS.register("whisperfrond_fern",()->new WhisperfrondBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_FERN)));
    public static final DeferredBlock<ManaheartPlantBlock> DAWNSTAR=BLOCKS.register("dawnstar_blossom",()->new ManaheartPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY).lightLevel(s->1)));
    public static final DeferredBlock<ManaheartPlantBlock> MOONVEIL=BLOCKS.register("moonveil_bloom",()->new ManaheartPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AZURE_BLUET).lightLevel(s->1)));
    public static final DeferredBlock<ManaheartPlantBlock> SHRUB=BLOCKS.register("tangleveil_shrub",()->new ManaheartPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA)));
    static {for(var block:java.util.List.of(MOSS,CARPET,BOULDER,FERN,DAWNSTAR,MOONVEIL,SHRUB))ITEMS.registerSimpleBlockItem(block);}
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);FEATURES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.NATURAL_BLOCKS)for(var block:java.util.List.of(MOSS,CARPET,BOULDER,FERN,DAWNSTAR,MOONVEIL,SHRUB))event.accept(block.get());
        });
    }
    private ManaheartFlora(){}
}
