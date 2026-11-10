package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.Azkerash.riftfall.BackstabSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the authored face-down corpse pose on the client; server death and despawn now run normally. */
@Mixin(LivingEntity.class)
public abstract class BackstabCorpseMixin {
    @Inject(method="tickDeath",at=@At("HEAD"),cancellable=true)
    private void riftfall$finishDrop(CallbackInfo ci){
        if(BackstabSequence.holdsCorpse((LivingEntity)(Object)this)){((LivingEntity)(Object)this).deathTime=0;ci.cancel();}
    }
}
