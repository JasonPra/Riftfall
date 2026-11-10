package org.Azkerash.riftfall.mixin;

import net.minecraft.world.entity.Mob;
import org.Azkerash.riftfall.stamina.MobStamina;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Pay once per Epic Fight animation, including attacks with several phases or several victims. */
@Mixin(value=AttackAnimation.class,remap=false)
public abstract class MobAttackStaminaMixin {
    @Inject(method="begin",at=@At("HEAD"))
    private void riftfall$begin(LivingEntityPatch<?> patch,CallbackInfo ci){
        if(!patch.isLogicalClient() && patch.getOriginal() instanceof Mob mob)
            MobStamina.automaticAnimation(mob,(AttackAnimation)(Object)this);
    }
}
