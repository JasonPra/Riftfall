package org.Azkerash.riftfall.mixin;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.Azkerash.riftfall.worldmana.geysers.*;

@Mixin(ChunkStatusTasks.class)
public abstract class GeyserBiomeGenerationMixin {
    @Inject(method="generateStructureStarts",at=@At("HEAD"),cancellable=true)
    private static void riftfall$excludeStructureStarts(WorldGenContext context,ChunkStep step,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess chunk,CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir){
        var cp=chunk.getPos();if(GeyserBiomes.landscapeField(context.level(),cp.x,cp.z)==null)return;
        GoblinCampStructure.generateStart(context.level(),context.generator(),chunk);
        context.level().onStructureStartsAvailable(chunk);
        cir.setReturnValue(CompletableFuture.completedFuture(chunk));
    }
    @Inject(method="generateSurface",at=@At("RETURN"),cancellable=true)
    private static void riftfall$shapeSurface(WorldGenContext context,ChunkStep step,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess chunk,CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir){
        cir.setReturnValue(cir.getReturnValue().thenApply(result->{
            var cp=result.getPos();var field=GeyserBiomes.landscapeField(context.level(),cp.x,cp.z);
            if(field!=null)GeyserTerrain.reshape(new net.minecraft.server.level.WorldGenRegion(context.level(),cache,step,result),field,GeyserLayout.mix(context.level().getSeed()^0x474559534552L),cp.x,cp.z);
            return result;
        }));
    }
    @Inject(method="generateCarvers",at=@At("RETURN"),cancellable=true)
    private static void riftfall$carveNetwork(WorldGenContext context,ChunkStep step,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess chunk,CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir){
        cir.setReturnValue(cir.getReturnValue().thenApply(result->{
            var cp=result.getPos();if(GeyserBiomes.landscapeField(context.level(),cp.x,cp.z)==null)return result;
            var world=new net.minecraft.server.level.WorldGenRegion(context.level(),cache,step,result);
            var feature=world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("riftfall","geyser_caves"));
            if(GeyserBiomes.field(context.level(),cp.x,cp.z)!=null)feature.place(world,context.generator(),net.minecraft.util.RandomSource.create(GeyserLayout.mix(world.getSeed()^cp.toLong())),cp.getWorldPosition());
            GeyserTerrain.finishSurface(world,cp.x,cp.z,GeyserLayout.mix(world.getSeed()^0x474559534552L));
            return result;
        }));
    }
}
