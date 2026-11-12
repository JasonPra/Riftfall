package org.Azkerash.riftfall.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.Azkerash.riftfall.worldmana.geysers.GeyserBiomes;

/** Also prevent pieces from structures whose starting chunk lies outside the field. */
@Mixin(StructureStart.class)
public abstract class GeyserStructurePlacementMixin {
    @Inject(method="placeInChunk",at=@At("HEAD"),cancellable=true)
    private void riftfall$excludeGeyserPieces(WorldGenLevel world,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bounds,ChunkPos chunk,CallbackInfo ci){
        if(world instanceof net.minecraft.server.level.WorldGenRegion&&GeyserBiomes.field(world.getLevel(),chunk.x,chunk.z)!=null)ci.cancel();
    }
}
