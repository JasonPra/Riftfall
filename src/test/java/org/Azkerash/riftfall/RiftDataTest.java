package org.Azkerash.riftfall;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftDataTest {
    @Test void restartPreservesActiveAndBrokenEncounters() {
        RiftData original = new RiftData();
        original.nextId = 8;
        original.spawnTicks = 123;
        RiftData.Rift waiting = new RiftData.Rift(6, new BlockPos(-123, 72, 456), 73);
        RiftData.Rift broken = new RiftData.Rift(7, new BlockPos(10, 64, -30), 0);
        broken.built = true;
        broken.waveTicks = 81;
        UUID enemy = UUID.randomUUID();
        UUID escaped = UUID.randomUUID();
        broken.enemies.add(enemy);
        broken.outside.add(escaped);
        original.rifts.add(waiting);
        original.rifts.add(broken);

        RiftData restored = RiftData.load(original.save(new CompoundTag(), null), null);
        assertEquals(8, restored.nextId);
        assertEquals(123, restored.spawnTicks);
        assertEquals(2, restored.rifts.size());
        assertEquals(waiting.pos, restored.rifts.getFirst().pos);
        assertEquals(73, restored.rifts.getFirst().remaining);
        assertFalse(restored.rifts.getFirst().built);
        RiftData.Rift encounter = restored.rifts.get(1);
        assertEquals(7, encounter.id);
        assertEquals(0, encounter.remaining);
        assertTrue(encounter.built);
        assertEquals(81, encounter.waveTicks);
        assertEquals(java.util.Set.of(enemy), encounter.enemies);
        assertEquals(java.util.Set.of(escaped), encounter.outside);
        assertEquals(broken.arena(), encounter.arena());
    }

    @Test void clearedRiftStaysClosedAfterRestart() {
        RiftData original = new RiftData();
        original.nextId = 42;
        RiftData restored = RiftData.load(original.save(new CompoundTag(), null), null);
        assertTrue(restored.rifts.isEmpty());
        assertEquals(42, restored.nextId, "Cleared arena IDs must never be reused");
    }
}
