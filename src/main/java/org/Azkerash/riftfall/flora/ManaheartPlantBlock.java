package org.Azkerash.riftfall.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

/** Ordinary replaceable, biome-decorated plants, including on the grove's own moss. */
public final class ManaheartPlantBlock extends BushBlock {
    public static final MapCodec<ManaheartPlantBlock> CODEC=simpleCodec(ManaheartPlantBlock::new);
    public ManaheartPlantBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override protected MapCodec<? extends BushBlock> codec(){return CODEC;}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos){
        return super.mayPlaceOn(state,level,pos)||state.is(ManaheartFlora.MOSS.get())||state.is(ManaheartFlora.BOULDER.get());
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        var offset=state.getOffset(level,pos);return box(3,0,3,13,12,13).move(offset.x,offset.y,offset.z);
    }
}
