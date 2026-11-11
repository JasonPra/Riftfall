package org.Azkerash.riftfall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.Azkerash.riftfall.client.CrystalGroupRenderUpdates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class CrystalGroupUpdatesMixin {
    @Inject(method="onBlockStateChange",at=@At("TAIL"))
    private void riftfall$refreshCrystalGroup(BlockPos pos,BlockState old,BlockState next,CallbackInfo ci){CrystalGroupRenderUpdates.changed((Level)(Object)this,pos,old,next);}
}
