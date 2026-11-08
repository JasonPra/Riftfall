package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

final class CrystalGrowth {
    static boolean canFund(double points, double criticalMass, double cost) {
        return (points > 100 || points >= criticalMass) && points >= cost;
    }

    static void grow(ServerLevel world, BlockPos pos, BlockState state) {
        if (state.getValue(CrystalClusterBlock.CRYSTALS) < CrystalClusterBlock.MAX_CRYSTALS) {
            double cost = Config.CRYSTAL_GROWTH_COST.get();
            if (SoulEssenceEvents.canSpend(world, pos, cost)
                && world.setBlock(pos, state.cycle(CrystalClusterBlock.CRYSTALS), 3)) SoulEssenceEvents.spend(world, pos, cost);
            return;
        }
        Direction facing = state.getValue(CrystalClusterBlock.FACING);
        // One seed per available direction. Offset candidates wrap around corners so
        // colonies can climb walls and reach ceilings rather than only covering floors.
        for (Direction direction : Direction.values()) {
            if (!SoulEssenceEvents.canSpend(world, pos, Config.CRYSTAL_SEED_COST.get())) break;
            BlockPos adjacent = pos.relative(direction);
            for (BlockPos target : new BlockPos[]{adjacent, adjacent.relative(facing.getOpposite()), adjacent.relative(facing)}) {
                BlockState seed = seedState(world, target, facing);
                if (seed != null && world.setBlock(target, seed, 3)) {
                    SoulEssenceEvents.spend(world, pos, Config.CRYSTAL_SEED_COST.get());
                    break;
                }
            }
        }
    }

    static BlockState seedState(LevelAccessor level, BlockPos pos, Direction preferred) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return null;
        if (!available(level, pos) || !level.getBlockState(pos).isAir()) return null;
        for (int i = -1; i < Direction.values().length; i++) {
            Direction facing = i == -1 ? preferred : Direction.values()[i];
            if (!available(level, pos.relative(facing.getOpposite()))) continue;
            BlockState seed = RiftBlocks.CRYSTAL_CLUSTER.get().defaultBlockState().setValue(CrystalClusterBlock.FACING, facing);
            if (seed.canSurvive(level, pos)) return CrystalClusterBlock.connections(seed, level, pos);
        }
        return null;
    }

    private static boolean available(LevelAccessor level, BlockPos pos) {
        return !(level instanceof ServerLevel world)
            || (world.getWorldBorder().isWithinBounds(pos) && world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null);
    }

    static BlockPos caveSite(LevelAccessor level, ChunkPos chunk, RandomSource random, Heightmap.Types heightmap) {
        for (int attempt = 0; attempt < 64; attempt++) {
            int x = chunk.getMinBlockX() + 4 + random.nextInt(8);
            int z = chunk.getMinBlockZ() + 4 + random.nextInt(8);
            int min = level.getMinBuildHeight() + 8;
            int max = Math.min(32, level.getHeight(heightmap, x, z) - 8);
            if (max < min) continue;
            BlockPos pos = new BlockPos(x, min + random.nextInt(max - min + 1), z);
            if (seedState(level, pos, Direction.UP) != null) return pos;
        }
        return null;
    }

    private CrystalGrowth() {}
}
