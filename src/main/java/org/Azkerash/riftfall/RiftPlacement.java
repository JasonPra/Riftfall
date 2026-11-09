package org.Azkerash.riftfall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

final class RiftPlacement {
    static final int CHUNK_RADIUS = 63; // Approximately 1,008 blocks.

    static boolean tooClose(ChunkPos first, ChunkPos second) {
        long x = (long) first.x - second.x, z = (long) first.z - second.z;
        return Math.abs(x) <= CHUNK_RADIUS && Math.abs(z) <= CHUNK_RADIUS
            && x * x + z * z <= CHUNK_RADIUS * CHUNK_RADIUS;
    }

    static boolean available(RiftData data, BlockPos pos) { return available(data, new ChunkPos(pos)); }
    static boolean available(RiftData data, ChunkPos pos) { return data.placement.available(pos); }

    /** Saved roots never move or disappear when dormant; rebuild this index on load. */
    static final class Index {
        private static final int CELL_SIZE = 64;
        private final Map<Long, List<ChunkPos>> cells = new HashMap<>();
        private final Set<Long> roots = new HashSet<>();

        void add(ChunkPos root) {
            if (roots.add(root.toLong())) cells.computeIfAbsent(cell(root), key -> new ArrayList<>()).add(root);
        }

        boolean available(ChunkPos pos) {
            int x = Math.floorDiv(pos.x, CELL_SIZE), z = Math.floorDiv(pos.z, CELL_SIZE);
            // Radius fits in one cell: only these nine buckets can contain a blocker.
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                var nearby = cells.get(ChunkPos.asLong(x + dx, z + dz));
                if (nearby != null) for (ChunkPos root : nearby) if (tooClose(root, pos)) return false;
            }
            return true;
        }

        private static long cell(ChunkPos pos) {
            return ChunkPos.asLong(Math.floorDiv(pos.x, CELL_SIZE), Math.floorDiv(pos.z, CELL_SIZE));
        }
    }
    private RiftPlacement() {}
}
