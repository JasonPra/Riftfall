package org.Azkerash.riftfall.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import org.Azkerash.riftfall.stamina.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Every vanilla/modded Mob inherits the shared pool; species may override its integration methods. */
@Mixin(Mob.class)
public abstract class MobStaminaMixin implements StaminaUser {
    @Unique private StaminaState riftfall$stamina;
    @Override public StaminaState staminaState(){if(riftfall$stamina==null)riftfall$stamina=new StaminaState();return riftfall$stamina;}
    @Override public void syncPanting(boolean panting){}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void riftfall$save(CompoundTag data,CallbackInfo ci){MobStamina.save((Mob)(Object)this,data);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void riftfall$load(CompoundTag data,CallbackInfo ci){MobStamina.load((Mob)(Object)this,data);}
    @Inject(method="doHurtTarget",at=@At("HEAD"),cancellable=true)
    private void riftfall$attack(Entity target,CallbackInfoReturnable<Boolean> cir){
        var mob=(Mob)(Object)this;
        if(!mob.level().isClientSide && !MobStamina.automaticContact(mob,"swipe"))cir.setReturnValue(false);
    }
}
