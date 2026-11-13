package org.Azkerash.riftfall.wood;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
/** Only partially stripped segments need a block entity; ordinary trees have none. */
public final class WoundedLegacyBranch extends LegacyBranchBlock implements EntityBlock {
    public WoundedLegacyBranch(ResourceLocation name,BlockBehaviour.Properties properties){super(name,properties);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new LegacyBarkWound(pos,state);}
}
