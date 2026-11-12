package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small generation-time camp, wholly contained in one chunk; goblins come only from its spawner. */
public final class GoblinCampFeature extends Feature<NoneFeatureConfiguration> {
    public GoblinCampFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world = context.level();
        int cx = context.origin().getX() >> 4, cz = context.origin().getZ() >> 4;
        var field = GeyserBiomes.field(world.getLevel(), cx, cz);
        if (field == null || field.zone(cx, cz) != GeyserZone.OUTER
            || !GoblinCampLayout.eligible(world.getSeed(), field)
            || !GoblinCampLayout.candidate(world.getSeed(), cx, cz)) return false;
        var center = new BlockPos(cx * 16 + 8, 0, cz * 16 + 8);
        int floor = suitableFloor(world, center);
        if (floor == Integer.MIN_VALUE) return false;
        build(world, center.atY(floor + 1));
        return true;
    }
    /** Validate the entire footprint before writing: dry, open, gently graded grassland. */
    static int suitableFloor(WorldGenLevel world, BlockPos center) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 5; dz++) {
            var p = world.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, center.offset(dx, 0, dz));
            int floor = p.getY() - 1; min = Math.min(min, floor); max = Math.max(max, floor);
            if (max - min > 1 || !world.getBiome(p).is(GeyserBiomes.key(GeyserZone.OUTER))
                || !world.getBlockState(p.below()).is(BlockTags.DIRT)) return Integer.MIN_VALUE;
            for (int dy = 0; dy <= 5; dy++) {
                var state = world.getBlockState(p.above(dy));
                if (!state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)
                    || !(state.isAir() || state.canBeReplaced())) return Integer.MIN_VALUE;
            }
        }
        return max;
    }
    static void build(WorldGenLevel world, BlockPos origin) {
        // Level at most one block, never excavate a platform into a hillside or river.
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 5; dz++) {
            var p = origin.offset(dx, 0, dz);
            if (world.getBlockState(p.below()).isAir() || world.getBlockState(p.below()).canBeReplaced())
                put(world, p.below(), Blocks.DIRT);
            for (int dy = 0; dy <= 4; dy++) if (!world.getBlockState(p.above(dy)).isAir()) put(world, p.above(dy), Blocks.AIR);
            if (dx * dx + (dz - 1) * (dz - 1) < 28 || dz < -2)
                put(world, p.below(), ((dx + dz) & 3) == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
        }
        tent(world, origin.offset(-4, 0, -3), Blocks.BROWN_WOOL);
        tent(world, origin.offset(3, 0, -3), Blocks.GREEN_WOOL);
        var fire = origin.offset(1, 0, 2);
        world.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
        for (int dx = -2; dx <= 0; dx++) world.setBlock(origin.offset(dx, 0, 4),
            Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 2);
        put(world, origin.offset(5, 0, 2), Blocks.BARREL);
        put(world, origin.offset(5, 1, 2), Blocks.HAY_BLOCK);
        put(world, origin.offset(-5, 0, 2), Blocks.CRAFTING_TABLE);
        var spawnerPos = origin.offset(-4, 0, -3);
        put(world, spawnerPos, Blocks.SPAWNER);
        if (world.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            // No live Level during decoration: its block-update callback would request this unfinished chunk.
            spawner.getSpawner().load(null, spawnerPos, spawnerData());
            spawner.setChanged();
        }
    }
    private static void tent(WorldGenLevel world, BlockPos center, Block canvas) {
        for (int z = -2; z <= 1; z++) {
            for (int x : new int[]{-2, 2}) {
                put(world, center.offset(x, 0, z), canvas);
                put(world, center.offset(x, 1, z), canvas);
            }
            for (int x : new int[]{-1, 1}) put(world, center.offset(x, 2, z), canvas);
            put(world, center.offset(0, 3, z), canvas);
        }
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 1; y++) put(world, center.offset(x, y, -2), canvas);
        for (int z : new int[]{-2, 1}) put(world, center.offset(0, 2, z), Blocks.SPRUCE_FENCE);
    }
    static CompoundTag spawnerData() {
        var tag = new CompoundTag();
        tag.putShort("Delay", (short)100);
        tag.putShort("MinSpawnDelay", (short)400); tag.putShort("MaxSpawnDelay", (short)800);
        tag.putShort("SpawnCount", (short)1); tag.putShort("MaxNearbyEntities", (short)6);
        tag.putShort("RequiredPlayerRange", (short)16); tag.putShort("SpawnRange", (short)4);
        var potentials = new ListTag();
        String[] types = {"goblin", "goblin_archer", "goblin_shaman"}; int[] weights = {6, 3, 1};
        for (int i = 0; i < types.length; i++) {
            var entity = new CompoundTag(); entity.putString("id", "riftfall:" + types[i]);
            var data = new CompoundTag(); data.put("entity", entity);
            // Camp goblins are living creatures: daylight does not disable their spawner.
            var light = new CompoundTag(); var range = new CompoundTag(); range.putInt("min_inclusive", 0); range.putInt("max_inclusive", 15);
            light.put("block_light_limit", range.copy()); light.put("sky_light_limit", range.copy()); data.put("custom_spawn_rules", light);
            var entry = new CompoundTag(); entry.putInt("weight", weights[i]); entry.put("data", data); potentials.add(entry);
            if (i == 0) tag.put("SpawnData", data.copy());
        }
        tag.put("SpawnPotentials", potentials);
        return tag;
    }
    private static void put(WorldGenLevel world, BlockPos pos, Block block) { world.setBlock(pos, block.defaultBlockState(), 2); }
}
