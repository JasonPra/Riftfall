package org.Azkerash.riftfall.mixin;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.Azkerash.riftfall.worldmana.geysers.GeyserBiomes;

/** Include zone palettes in feature sorting and decoration, not just in saved chunk biome data. */
@Mixin(BiomeSource.class)
public abstract class GeyserBiomeSourceMixin {
    @Inject(method="possibleBiomes",at=@At("RETURN"),cancellable=true)
    private void riftfall$possibleGeyserBiomes(CallbackInfoReturnable<Set<Holder<Biome>>> cir){
        cir.setReturnValue(GeyserBiomes.possible((BiomeSource)(Object)this,cir.getReturnValue()));
    }
}
