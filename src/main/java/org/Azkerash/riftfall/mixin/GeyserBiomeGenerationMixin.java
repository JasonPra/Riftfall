package org.Azkerash.riftfall.mixin;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.Azkerash.riftfall.worldmana.geysers.GeyserBiomes;

@Mixin(ChunkStatusTasks.class)
public abstract class GeyserBiomeGenerationMixin {
    @Inject(method="generateBiomes",at=@At("RETURN"),cancellable=true)
    private static void riftfall$geyserBiomes(WorldGenContext context,ChunkStep step,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess chunk,CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir){
        cir.setReturnValue(cir.getReturnValue().thenApply(result->GeyserBiomes.assign(context.level(),result)));
    }
}
