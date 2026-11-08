package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the cluster's identity for future features; currently has no ticker or interaction. */
public final class CrystalClusterBlockEntity extends BlockEntity {
    public CrystalClusterBlockEntity(BlockPos pos, BlockState state) {
        super(RiftBlocks.CRYSTAL_CLUSTER_ENTITY.get(), pos, state);
    }
}
