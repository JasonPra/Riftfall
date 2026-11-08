package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;

final class RiftPlacement {
    static final int RADIUS = 1000;
    static boolean tooClose(BlockPos first, BlockPos second) {
        double x = (double) first.getX() - second.getX(), z = (double) first.getZ() - second.getZ();
        return x * x + z * z <= (double) RADIUS * RADIUS;
    }
    static boolean available(RiftData data, BlockPos pos) {
        return data.rifts.stream().noneMatch(r -> tooClose(r.pos, pos));
    }
    private RiftPlacement() {}
}
