package org.Azkerash.riftfall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.Azkerash.riftfall.worldmana.geysers.*;

@Mixin(WorldGenRegion.class)
public abstract class GeyserPlantPlacementMixin {
    @Inject(method="setBlock",at=@At("HEAD"),cancellable=true)
    private void riftfall$validGeyserPlants(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> cir){
        if(!GeyserPlantRules.plant(state))return;
        var world=(WorldGenRegion)(Object)this;
        if(GeyserBiomes.field(world.getLevel(),pos.getX()>>4,pos.getZ()>>4)!=null&&!GeyserPlantRules.valid(world,pos,state))cir.setReturnValue(false);
    }
    // Features often skip neighbor updates when replacing a supporting block or a tall plant's lower half.
    @Inject(method="setBlock",at=@At("RETURN"))
    private void riftfall$preservePlantSupport(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValue())return;
        var world=(WorldGenRegion)(Object)this;
        if(!world.getChunk(pos).getPersistedStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.CARVERS))return;
        var above=pos.above();var plant=world.getBlockState(above);
        if(GeyserPlantRules.plant(plant)&&GeyserBiomes.field(world.getLevel(),pos.getX()>>4,pos.getZ()>>4)!=null&&!GeyserPlantRules.valid(world,above,plant))
            world.setBlock(above,plant.getFluidState().createLegacyBlock(),2);
    }
}
