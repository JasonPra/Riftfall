package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
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
        BlockPos pos = CrystalGrowth.caveSite(level, new ChunkPos(context.origin()), context.random(), Heightmap.Types.WORLD_SURFACE_WG);
        if (pos == null) return false;
        var seed = CrystalGrowth.seedState(level, pos, Direction.UP);
        if (seed == null || !level.setBlock(pos, seed, 2)) return false;
        // Credit only actual world generation. Player placement and paid spreading
        // never create essence, and reloading a generated chunk never credits it again.
        ChunkAccess chunk = level.getChunk(pos);
        SoulEssence previous = chunk.getData(SoulEssenceEvents.ESSENCE);
        chunk.setData(SoulEssenceEvents.ESSENCE, new SoulEssence(previous.points() + 50));
        chunk.setUnsaved(true);
        return true;
    }
}
