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
        original.spawnChunkSeeded = true;
        RiftData.Rift waiting = new RiftData.Rift(6, new BlockPos(-123, 72, 456), 73);
        RiftData.Rift broken = new RiftData.Rift(7, new BlockPos(10, 64, -30), 0);
        broken.exteriorYaw = 123.45F;
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
        assertTrue(restored.spawnChunkSeeded);
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
        assertEquals(123.45F, encounter.portalYaw(false));
        assertEquals(180F, encounter.portalYaw(true));
        assertEquals(encounter.arena().offset(0, 1, 8), encounter.exit());
    }

    @Test void olderSavesKeepTheirExteriorOrientationAndUseTheSouthExit() {
        RiftData original = new RiftData();
        original.rifts.add(new RiftData.Rift(2, new BlockPos(0, 64, 0), 100));
        CompoundTag tag = original.save(new CompoundTag(), null);
        tag.getList("rifts", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).remove("exteriorYaw");
        tag.remove("spawnChunkSeeded");
        RiftData data = RiftData.load(tag, null);
        assertFalse(data.spawnChunkSeeded, "Older worlds receive the initial soul essence bonus once");
        RiftData.Rift restored = data.rifts.getFirst();
        assertEquals(0F, restored.portalYaw(false));
        assertEquals(180F, restored.portalYaw(true));
        assertEquals(new BlockPos(128, 81, 8), restored.exit());
    }

    @Test void clearedRiftStaysClosedAfterRestart() {
        RiftData original = new RiftData();
        original.nextId = 42;
        RiftData restored = RiftData.load(original.save(new CompoundTag(), null), null);
        assertTrue(restored.rifts.isEmpty());
        assertEquals(42, restored.nextId, "Cleared arena IDs must never be reused");
    }
    @Test void dormantRiftPreservesItsIdentityAndProgression() {
        RiftData original = new RiftData();
        RiftData.Rift rift = new RiftData.Rift(3, new BlockPos(-32, 64, 48), 0);
        rift.built = true; rift.cleared = true; rift.dormant = true;
        rift.dormantTicks = 1200; rift.essence = 150; rift.energy = 345.5; rift.refilling = true;
        UUID visitor = UUID.randomUUID(); rift.occupants.add(visitor);
        UUID pendingRetirement = UUID.randomUUID(); rift.retired.add(pendingRetirement);
        rift.cycle = 4; rift.encounterLevel = 3; rift.radius = 14;
        original.rifts.add(rift);
        RiftData restored = RiftData.load(original.save(new CompoundTag(), null), null);
        RiftData.Rift saved = restored.rifts.getFirst();
        assertEquals(0, restored.activeCount());
        assertEquals(rift.pos, saved.pos);
        assertTrue(saved.cleared && saved.dormant && saved.built);
        assertEquals(1200, saved.dormantTicks);
        assertEquals(150, saved.essence); assertEquals(345.5, saved.energy); assertTrue(saved.refilling);
        assertEquals(java.util.Set.of(visitor), saved.occupants);
        assertEquals(java.util.Set.of(pendingRetirement), saved.retired);
        assertTrue(saved.spawned.isEmpty(), "Runtime spawn references are rebuilt rather than serialized");
        assertEquals(4, saved.cycle); assertEquals(3, saved.encounterLevel);
        assertEquals(14, saved.radius);
        assertEquals(saved.arena().offset(0, 1, 12), saved.exit());
        assertEquals(saved.arena().offset(0, 1, 9), saved.entry());
    }

}
