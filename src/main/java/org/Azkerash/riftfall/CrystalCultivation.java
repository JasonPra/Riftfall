package org.Azkerash.riftfall;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Tracks loaded mature block entities without scanning entire chunks each second. */
final class CrystalCultivation {
    private static final Map<ServerLevel, Map<Long, Set<BlockPos>>> MATURE = new HashMap<>();

    static void update(ServerLevel world, BlockPos pos, boolean mature) {
        if (mature) MATURE.computeIfAbsent(world, key -> new HashMap<>())
            .computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), key -> new HashSet<>()).add(pos.immutable());
        else {
            var chunks = MATURE.get(world);
            if (chunks == null) return;
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            Set<BlockPos> positions = chunks.get(key);
            if (positions != null && positions.remove(pos) && positions.isEmpty()) chunks.remove(key);
            if (chunks.isEmpty()) MATURE.remove(world);
        }
    }

    static int count(ServerLevel world, ChunkPos pos) {
        var chunks = MATURE.get(world);
        return chunks == null || !chunks.containsKey(pos.toLong()) ? 0 : chunks.get(pos.toLong()).size();
    }

    static double rate(double base, double bonus, int mature) { return base + bonus * mature; }
    static void clear() { MATURE.clear(); }
    private CrystalCultivation() {}
}
