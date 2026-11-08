package org.Azkerash.riftfall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrystalWorldgen extends Feature<NoneFeatureConfiguration> {
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Riftfall.MODID);
    static {
        FEATURES.register("soul_crystals", CrystalWorldgen::new);
    }

    private CrystalWorldgen() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        ChunkPos origin = new ChunkPos(context.origin());
        LinkedHashMap<BlockPos, BlockState> patch = null;
        // Search for enough exposed cave surface to establish a colony rather than
        // generating an isolated seed in a cramped crevice. Stay inside this chunk.
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos site = CrystalGrowth.caveSite(level, origin, context.random(), Heightmap.Types.WORLD_SURFACE_WG);
            if (site == null) continue;
            var candidate = patch(level, origin, site, 11 + context.random().nextInt(5));
            if (candidate.size() >= 8) { patch = candidate; break; }
        }
        if (patch == null) return false;
        int adults = 2 + context.random().nextInt(2), placed = 0;
        List<BlockPos> generated = new ArrayList<>();
        for (var entry : patch.entrySet()) {
            int crystals = placed < adults ? CrystalClusterBlock.MAX_CRYSTALS : 1 + context.random().nextInt(2);
            if (level.setBlock(entry.getKey(), entry.getValue().setValue(CrystalClusterBlock.CRYSTALS, crystals), 2)) {
                generated.add(entry.getKey());
                placed++;
            }
        }
        // Worldgen suppresses neighbor updates. Finalize both ends of every vein
        // after the whole patch exists, so connections are correct on first load.
        for (BlockPos pos : generated) level.setBlock(pos,
            CrystalClusterBlock.connections(level.getBlockState(pos), level, pos), 2);
        if (placed == 0) return false;
        // One credit per colony, not per crystal: larger decorative patches must
        // not manufacture unbounded starting essence.
        ChunkAccess chunk = level.getChunk(generated.getFirst());
        SoulEssence previous = chunk.getData(SoulEssenceEvents.ESSENCE);
        chunk.setData(SoulEssenceEvents.ESSENCE, new SoulEssence(previous.points() + 50));
        chunk.setUnsaved(true);
        return true;
    }

    private static LinkedHashMap<BlockPos, BlockState> patch(
            LevelAccessor level, ChunkPos chunk, BlockPos origin, int limit) {
        var result = new LinkedHashMap<BlockPos, BlockState>();
        var first = CrystalGrowth.seedState(level, origin, Direction.UP);
        if (first == null) return result;
        result.put(origin, first);
        var queue = new ArrayDeque<BlockPos>();
        queue.add(origin);
        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos pos = queue.removeFirst();
            Direction facing = result.get(pos).getValue(CrystalClusterBlock.FACING);
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = pos.relative(direction);
                for (BlockPos target : new BlockPos[]{adjacent, adjacent.relative(facing.getOpposite()), adjacent.relative(facing)}) {
                    if (result.size() >= limit) break;
                    if (result.containsKey(target) || !new ChunkPos(target).equals(chunk)
                        || Math.abs(target.getX() - origin.getX()) > 4 || Math.abs(target.getY() - origin.getY()) > 3
                        || Math.abs(target.getZ() - origin.getZ()) > 4) continue;
                    var state = CrystalGrowth.seedState(level, target, facing);
                    if (state != null) { result.put(target.immutable(), state); queue.addLast(target.immutable()); break; }
                }
            }
        }
        return result;
    }
}
