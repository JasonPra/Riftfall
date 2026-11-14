package org.Azkerash.riftfall.wood;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** A hanging organ beneath living Heartwood leaves: bud, blossom, silver fruit and ripe gold fruit. */
public final class HeartwoodFruitBlock extends Block implements EntityBlock {
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,3);
    private static final VoxelShape BUD=Block.box(6,7,6,10,16,10),FLOWER=Block.box(4,12,4,12,16,12),FRUIT=Block.box(4,1,4,12,16,12);
    public HeartwoodFruitBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(STAGE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(STAGE);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(STAGE)){case 0->BUD;case 1->FLOWER;default->FRUIT;};}
    public static boolean supported(LevelReader level,BlockPos pos){
        var state=level.getBlockState(pos.above());
        return isHeartwoodLeaf(state);
    }
    public static boolean isHeartwoodLeaf(BlockState state){
        var species=com.dtteam.dynamictrees.tree.species.Species.findSpecies(LegacyTrees.ID);
        return species.isValid()&&species.getLeavesProperties().getDynamicLeavesBlock().filter(state::is).isPresent();
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return supported(level,pos);}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos from){
        return direction==Direction.UP&&!supported(level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,direction,neighbor,level,pos,from);
    }
    public static boolean harvest(ServerLevel level,BlockPos pos,Player player){
        var state=level.getBlockState(pos);if(!state.is(LegacyWood.FRUIT_BLOCK.get())||state.getValue(STAGE)!=3)return false;
        var stack=new ItemStack(LegacyWood.FRUIT.get());
        if(player==null)Block.popResource(level,pos,stack);
        else if(!player.getInventory().add(stack))player.drop(stack,false);
        if(level.getBlockEntity(pos) instanceof HeartwoodFruitEntity fruit)fruit.restart();
        level.setBlock(pos,state.setValue(STAGE,0),3);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,net.minecraft.sounds.SoundSource.BLOCKS,.8f,1.0f);
        level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos);return true;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(state.getValue(STAGE)!=3)return InteractionResult.PASS;
        if(level instanceof ServerLevel server)harvest(server,pos,player);return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(state.getValue(STAGE)!=3)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(level instanceof ServerLevel server)harvest(server,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult target,LevelReader level,BlockPos pos,Player player){return new ItemStack(LegacyWood.FRUIT.get());}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(state.getValue(STAGE)==1&&random.nextInt(4)==0)
            level.addParticle(ParticleTypes.END_ROD,pos.getX()+.5+(random.nextDouble()-.5)*.35,pos.getY()+.81,pos.getZ()+.5+(random.nextDouble()-.5)*.35,(random.nextDouble()-.5)*.01,-.005,(random.nextDouble()-.5)*.01);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HeartwoodFruitEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return !level.isClientSide&&type==HeartwoodFruitEntity.TYPE.get()?(world,pos,block,entity)->HeartwoodFruitEntity.tick((ServerLevel)world,pos,block,(HeartwoodFruitEntity)entity):null;
    }
}
