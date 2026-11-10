package org.Azkerash.riftfall.mixin;

import java.util.List;
import net.minecraft.world.entity.Entity;
import org.Azkerash.riftfall.shield.ShieldCollisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value=AttackAnimation.Phase.class,remap=false)
public abstract class ShieldPhaseContactsMixin {
    @Inject(method="getCollidingEntities",at=@At("RETURN"),cancellable=true)
    private void riftfall$shieldHits(LivingEntityPatch<?> attacker,AttackAnimation animation,float previous,float current,float speed,CallbackInfoReturnable<List<Entity>> ci) {
        ci.setReturnValue(ShieldCollisions.includeShieldContacts((AttackAnimation.Phase)(Object)this,ci.getReturnValue()));
    }
}
