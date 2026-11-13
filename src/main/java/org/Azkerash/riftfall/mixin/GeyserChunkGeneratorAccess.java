package org.Azkerash.riftfall.mixin;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ChunkGenerator.class)
public interface GeyserChunkGeneratorAccess {
    @Mutable @Accessor("biomeSource") void riftfall$biomeSource(BiomeSource source);
    @Mutable @Accessor("featuresPerStep") void riftfall$featuresPerStep(Supplier<List<FeatureSorter.StepFeatureData>> features);
}
