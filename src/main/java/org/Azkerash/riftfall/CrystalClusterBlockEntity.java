package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystalClusterBlockEntity extends BlockEntity {
    public CrystalClusterBlockEntity(BlockPos pos, BlockState state) {
        super(RiftBlocks.CRYSTAL_CLUSTER_ENTITY.get(), pos, state);
    }

    @Override public void onLoad() { super.onLoad(); refreshCultivation(); }
    @Override public void setBlockState(BlockState state) { super.setBlockState(state); refreshCultivation(); }
    @Override public void setRemoved() {
        if (level instanceof ServerLevel world) CrystalCultivation.update(world, worldPosition, false);
        super.setRemoved();
    }
    private void refreshCultivation() {
        if (level instanceof ServerLevel world) CrystalCultivation.update(world, worldPosition,
            !isRemoved() && getBlockState().getValue(CrystalClusterBlock.CRYSTALS) == CrystalClusterBlock.MAX_CRYSTALS);
    }

    static void tick(Level level, BlockPos pos, BlockState state, CrystalClusterBlockEntity entity) {
        if (!(level instanceof ServerLevel world) || world.dimension() != Level.OVERWORLD) return;
        long interval = Config.CRYSTAL_GROWTH_SECONDS.get() * 20L;
        if (Math.floorMod(world.getGameTime() + pos.asLong(), interval) == 0) CrystalGrowth.grow(world, pos, state);
    }
}
