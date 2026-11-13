package org.Azkerash.riftfall.wood;

import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.*;

/** Primitive material for the future Vermillion Legacy family, not a stand-alone living tree. */
public final class LegacyLogBlock extends RotatedPillarBlock {
    public static final BooleanProperty NATURAL=BooleanProperty.create("natural");
    private final Supplier<? extends Block> stripped;
    public LegacyLogBlock(Properties properties,Supplier<? extends Block> stripped){
        super(properties);this.stripped=stripped;registerDefaultState(defaultBlockState().setValue(NATURAL,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(NATURAL);}
    public static boolean canHarvest(ItemStack tool,BlockState state){return tool.is(ItemTags.AXES)&&tool.isCorrectToolForDrops(state);}
    @Override public BlockState getToolModifiedState(BlockState state,UseOnContext context,ItemAbility ability,boolean simulate){
        if(stripped!=null&&ability==ItemAbilities.AXE_STRIP){
            if(context.getClickedFace().getAxis()==state.getValue(AXIS))return null;
            if(!context.getItemInHand().canPerformAction(ability)||!(context.getPlayer()!=null&&context.getPlayer().isCreative())&&!canHarvest(context.getItemInHand(),state))return null;
            return stripped.get().defaultBlockState().setValue(AXIS,state.getValue(AXIS)).setValue(NATURAL,state.getValue(NATURAL));
        }
        return super.getToolModifiedState(state,context,ability,simulate);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(hit.getDirection().getAxis()==state.getValue(AXIS)&&stack.canPerformAction(ItemAbilities.AXE_STRIP))return ItemInteractionResult.FAIL;
        if(!player.isCreative()&&stack.is(ItemTags.AXES)&&!canHarvest(stack,state)){
            LegacyWoodBehavior.deflect(player,hand,pos);return ItemInteractionResult.FAIL;
        }
        return super.useItemOn(stack,state,level,pos,player,hand,hit);
    }
    /** Drops only for a real bark-to-stripped replacement, not previewing a tool action. */
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(stripped!=null&&replacement.is(stripped.get())&&!moving&&!level.isClientSide())Block.popResource(level,pos,new ItemStack(LegacyWood.BARK.get()));
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face){return 5;}
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face){return 5;}
}
