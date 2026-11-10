package org.Azkerash.riftfall.mixin;

import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.shield.ShieldCollisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.event.types.entity.TakeDamageEvent;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;

/** Replace only shield guards' direction gate; sword guards keep their original skill logic. */
@Mixin(value=GuardSkill.class,remap=false)
public abstract class ShieldGuardContactMixin {
    @Redirect(method="lambda$onInitiate$15",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;dot(Lnet/minecraft/world/phys/Vec3;)D"))
    private double riftfall$physicalShield(Vec3 direction,Vec3 facing,SkillContainer container,TakeDamageEvent.Income event) {
        var defender=container.getExecutor().getOriginal();
        return ShieldCollisions.held(defender)!=null?(ShieldCollisions.contact(defender,event.getDamageSource())?1:-1):direction.dot(facing);
    }
}
