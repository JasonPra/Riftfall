package org.Azkerash.riftfall.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.Azkerash.riftfall.client.CrystalBridgeRenderUpdates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Level.class)
public abstract class CrystalBridgeUpdatesMixin {
    @Inject(method="onBlockStateChange",at=@At("TAIL"))
    private void riftfall$bridgeEndpointChanged(BlockPos pos,BlockState oldState,BlockState newState,CallbackInfo ci){CrystalBridgeRenderUpdates.changed((Level)(Object)this,pos,oldState,newState);}
}
