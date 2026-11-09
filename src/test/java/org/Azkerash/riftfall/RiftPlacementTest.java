package org.Azkerash.riftfall;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftPlacementTest {
    @Test void indexMatchesFullDistanceSearchAcrossPositiveAndNegativeBucketEdges() {
        var random = new Random(7924);
        var roots = new ArrayList<ChunkPos>();
        var index = new RiftPlacement.Index();
        for (int i = 0; i < 1000; i++) {
            var root = new ChunkPos(random.nextInt(4001) - 2000, random.nextInt(4001) - 2000);
            roots.add(root); index.add(root);
        }
        for (int i = 0; i < 3000; i++) {
            var candidate = new ChunkPos(random.nextInt(4001) - 2000, random.nextInt(4001) - 2000);
            boolean expected = roots.stream().noneMatch(root -> {
                long x = (long) candidate.x - root.x, z = (long) candidate.z - root.z;
                return x*x + z*z <= 63*63;
            });
            assertEquals(expected, index.available(candidate), candidate.toString());
        }
    }

    @Test void blockOffsetsAndHeightWithinAChunkDoNotChangeTheReservation() {
        var data = new RiftData();
        data.addRift(new RiftData.Rift(0, new BlockPos(15, 80, 15), 1));
        assertFalse(RiftPlacement.available(data, new BlockPos(1023, -60, 0)));
        assertTrue(RiftPlacement.available(data, new BlockPos(1024, 300, 0)));
        assertFalse(RiftPlacement.available(data, new BlockPos(-1008, 300, 0)));
        assertTrue(RiftPlacement.available(data, new BlockPos(-1009, 80, 0)));
    }

    @Test void indexRebuildsFromSavedDormantRiftsAndUpdatesForNewRifts() {
        var data = new RiftData();
        var rift = new RiftData.Rift(0, new BlockPos(-17, 64, -17), 1);
        rift.dormant = true; data.addRift(rift);
        var loaded = RiftData.load(data.save(new CompoundTag(), null), null);
        assertFalse(RiftPlacement.available(loaded, rift.pos));
        BlockPos distant = new BlockPos(100000, 64, 100000);
        assertTrue(RiftPlacement.available(loaded, distant));
        loaded.addRift(new RiftData.Rift(1, distant, 1));
        assertFalse(RiftPlacement.available(loaded, distant.offset(16, 100, 0)));
    }
}
