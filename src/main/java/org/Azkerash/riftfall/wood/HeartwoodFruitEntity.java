package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Saved loaded-tree time; harvesting and natural shedding both start a fresh bud. */
public final class HeartwoodFruitEntity extends BlockEntity {
    public static final int FLOWER_TICKS=9600,SILVER_TICKS=19200,RIPE_TICKS=36000,DROP_TICKS=48000;
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"riftfall");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<HeartwoodFruitEntity>> TYPE=TYPES.register("heartwood_fruit",()->BlockEntityType.Builder.of(HeartwoodFruitEntity::new,LegacyWood.FRUIT_BLOCK.get()).build(null));
    private int elapsed,loadedTicks;
    private BlockPos root;
    public HeartwoodFruitEntity(BlockPos pos,BlockState state){super(TYPE.get(),pos,state);elapsed=startOf(state.getValue(HeartwoodFruitBlock.STAGE));}
    public static void register(IEventBus bus){TYPES.register(bus);}
    public static int startOf(int stage){return switch(stage){case 1->FLOWER_TICKS;case 2->SILVER_TICKS;case 3->RIPE_TICKS;default->0;};}
    public static int stage(int ticks){return ticks>=RIPE_TICKS?3:ticks>=SILVER_TICKS?2:ticks>=FLOWER_TICKS?1:0;}
    public void setRoot(BlockPos value){root=value.immutable();setChanged();}
    public void restart(){elapsed=0;loadedTicks=0;setChanged();}
    public int elapsed(){return elapsed;}
    public static void tick(ServerLevel level,BlockPos pos,BlockState state,HeartwoodFruitEntity fruit){
        if(++fruit.loadedTicks<200)return;fruit.loadedTicks=0;fruit.advance(level,200);
    }
    public boolean advance(ServerLevel level,int ticks){
        var state=getBlockState();
        if(!HeartwoodFruitBlock.supported(level,worldPosition)){
            Block.dropResources(state,level,worldPosition,this);level.setBlock(worldPosition,Blocks.AIR.defaultBlockState(),3);return false;
        }
        if(root==null)root=HeartwoodWoundHealing.findLivingRoot(level,worldPosition.above());
        if(root==null||!level.getChunkSource().hasChunk(root.getX()>>4,root.getZ()>>4)||!TreeHelper.isRooty(level.getBlockState(root)))return false;
        var species=TreeHelper.getExactSpecies(level,root);
        if(!species.isValid()||!species.getRegistryName().equals(LegacyTrees.ID))return false;
        elapsed=(int)Math.min(DROP_TICKS,(long)elapsed+Math.max(0,ticks));setChanged();
        if(elapsed>=DROP_TICKS){
            // The ripe item really falls as a normal collectible ItemEntity.
            if(state.getValue(HeartwoodFruitBlock.STAGE)!=3){state=state.setValue(HeartwoodFruitBlock.STAGE,3);level.setBlock(worldPosition,state,3);}
            HeartwoodFruitBlock.harvest(level,worldPosition,null);return true;
        }
        int next=stage(elapsed);
        if(state.getValue(HeartwoodFruitBlock.STAGE)!=next){level.setBlock(worldPosition,state.setValue(HeartwoodFruitBlock.STAGE,next),3);level.sendBlockUpdated(worldPosition,state,getBlockState(),3);}
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("FruitTicks",elapsed);if(root!=null)tag.putLong("LivingRoot",root.asLong());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);if(tag.contains("FruitTicks"))elapsed=Math.clamp(tag.getInt("FruitTicks"),0,DROP_TICKS);root=tag.contains("LivingRoot")?BlockPos.of(tag.getLong("LivingRoot")):null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
