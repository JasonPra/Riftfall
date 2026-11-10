package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.*;
import org.Azkerash.riftfall.stamina.MobStamina;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MobSprintStaminaMixin {
    @Inject(method="setSprinting",at=@At("HEAD"),cancellable=true)
    private void riftfall$sprint(boolean sprinting,CallbackInfo ci){
        if(sprinting && (Object)this instanceof Mob mob && !mob.level().isClientSide && !MobStamina.sprintAllowed(mob))ci.cancel();
    }
}
