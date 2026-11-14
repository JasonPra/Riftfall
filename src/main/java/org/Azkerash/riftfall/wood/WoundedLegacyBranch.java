package org.Azkerash.riftfall.wood;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
/** Only wounded segments need a block entity; healthy branches have none. */
public final class WoundedLegacyBranch extends LegacyBranchBlock implements EntityBlock {
    public WoundedLegacyBranch(ResourceLocation name,BlockBehaviour.Properties properties){super(name,properties);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        if(level.isClientSide||type!=LegacyBarkWound.TYPE.get())return null;
        return (world,pos,block,entity)->LegacyBarkWound.tick(world,pos,block,(LegacyBarkWound)entity);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new LegacyBarkWound(pos,state);}
}
