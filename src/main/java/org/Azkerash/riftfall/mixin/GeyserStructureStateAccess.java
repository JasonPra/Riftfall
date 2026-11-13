package org.Azkerash.riftfall.mixin;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ChunkGeneratorStructureState.class)
public interface GeyserStructureStateAccess {
    @Mutable @Accessor("biomeSource") void riftfall$biomeSource(BiomeSource source);
}
