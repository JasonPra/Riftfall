package org.Azkerash.riftfall;

import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoulEssenceTest {
    @Test void passiveGainReachesCriticalMassWithoutOverflow() {
        SoulEssence essence = new SoulEssence(599.75);
        assertFalse(essence.isCritical(600));
        essence = essence.accumulate(1, 600);
        assertEquals(600, essence.points());
        assertTrue(essence.isCritical(600));
        assertEquals(600, essence.accumulate(Double.MAX_VALUE, 600).points());
    }

    @Test void chunkSerializationKeepsFractionalProgress() {
        SoulEssence original = new SoulEssence(17.25);
        var tag = SoulEssence.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        SoulEssence restored = SoulEssence.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        assertEquals(original, restored);
        assertEquals(17.75, restored.accumulate(.5, 600).points());
    }

    @Test void consumedChunkMustBuildUpAgain() {
        SoulEssence consumed = SoulEssence.EMPTY.accumulate(1, 600);
        assertEquals(1, consumed.points());
        assertFalse(consumed.isCritical(600));
        assertTrue(SoulEssence.CODEC.parse(NbtOps.INSTANCE, net.minecraft.nbt.DoubleTag.valueOf(-1)).error().isPresent());
    }
}
