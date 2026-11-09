package org.Azkerash.riftfall;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftReplenishmentTest {
    @Test void replacementRequiresFiveContinuousMinutesWithoutVisitors() {
        int timer = 0;
        for (int i = 0; i < 5999; i++) timer = RiftReplenishment.advance(timer, true, false);
        assertEquals(5999, timer);
        timer = RiftReplenishment.advance(timer, true, true);
        assertEquals(0, timer);
        for (int i = 0; i < 6000; i++) timer = RiftReplenishment.advance(timer, true, false);
        assertEquals(RiftReplenishment.INTERVAL, timer);
    }

    @Test void waitingForFundsDoesNotAccumulateABurstOfReplacements() {
        int timer = RiftReplenishment.INTERVAL;
        for (int i = 0; i < 12000; i++) timer = RiftReplenishment.advance(timer, true, false);
        assertEquals(RiftReplenishment.INTERVAL, timer);
        assertEquals(1, RiftReplenishment.advance(0, true, false));
        assertEquals(0, RiftReplenishment.advance(timer, false, false));
    }

    @Test void restartPreservesCapacityAndPartialAbsenceTimer() {
        var data = new RiftData();
        var r = new RiftData.Rift(2, BlockPos.ZERO, 1200);
        r.built = true; r.enemyCapacity = 6; r.replenishTicks = 4500;
        r.enemies.add(UUID.randomUUID()); data.addRift(r);
        var saved = RiftData.load(data.save(new CompoundTag(), null), null).rifts.getFirst();
        assertEquals(6, saved.enemyCapacity);
        assertEquals(4500, saved.replenishTicks);
        assertEquals(r.enemies, saved.enemies);
    }
}
