package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.worldmana.WorldMana;

public final class ManaCrystals {
    public static final TagKey<Block> SUPPORTS=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"mana_crystal_supports"));
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Riftfall.MODID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Riftfall.MODID);
    private static final Map<CrystalTier,DeferredBlock<ManaCrystalBlock>> CLUSTERS=new EnumMap<>(CrystalTier.class);
    private static final Map<CrystalTier,DeferredItem<BlockItem>> RAW=new EnumMap<>(CrystalTier.class);
    private static final Map<CrystalTier,DeferredItem<Item>> CUT=new EnumMap<>(CrystalTier.class);
    static {
        for(var tier:CrystalTier.values()){
            var block=BLOCKS.register(tier.id()+"_mana_crystal_cluster",()->new ManaCrystalBlock(tier,BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST_CLUSTER).noOcclusion().randomTicks().lightLevel(state->9).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
            CLUSTERS.put(tier,block);
            RAW.put(tier,ITEMS.register("raw_"+tier.id()+"_mana_crystal",()->new BlockItem(block.get(),new Item.Properties()){
                @Override public String getDescriptionId(){return "item.riftfall.raw_"+tier.id()+"_mana_crystal";}
            }));
            CUT.put(tier,ITEMS.registerSimpleItem("cut_"+tier.id()+"_mana_crystal"));
        }
    }
    public static ManaCrystalBlock block(CrystalTier tier){return CLUSTERS.get(tier).get();}
    public static Item raw(CrystalTier tier){return RAW.get(tier).get();}
    public static Item cut(CrystalTier tier){return CUT.get(tier).get();}
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);CrystalDeposits.register(bus);CrystalRefinement.register();
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{
            if(e.getTabKey()==CreativeModeTabs.NATURAL_BLOCKS)for(var tier:CrystalTier.values())e.accept(raw(tier));
            if(e.getTabKey()==CreativeModeTabs.INGREDIENTS)for(var tier:CrystalTier.values())e.accept(cut(tier));
        });
    }
    public static boolean canRefine(BlockState state){return state.getBlock() instanceof ManaCrystalBlock c&&c.tier().canRefine(state.getValue(ManaCrystalBlock.LAYERS));}
    /** Optional explicit refinement entry point; natural progression is handled by the geyser ledger. */
    public static boolean tryRefine(ServerLevel level,BlockPos pos,double cost){
        if(!Double.isFinite(cost)||cost<0)throw new IllegalArgumentException("Invalid refinement cost");
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)return false;
        var old=chunk.getBlockState(pos);if(!canRefine(old))return false;
        var next=block(((ManaCrystalBlock)old.getBlock()).tier().next().orElseThrow()).defaultBlockState().setValue(ManaCrystalBlock.LAYERS,8).setValue(ManaCrystalBlock.FACING,old.getValue(ManaCrystalBlock.FACING));
        if(!next.canSurvive(level,pos)||!WorldMana.tryExtract(chunk,cost))return false;
        if(level.setBlock(pos,next,Block.UPDATE_ALL))return true;
        WorldMana.add(chunk,cost);return false;
    }
    private ManaCrystals(){}
}
