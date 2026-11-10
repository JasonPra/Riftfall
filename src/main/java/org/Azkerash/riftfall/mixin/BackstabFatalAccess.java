package org.Azkerash.riftfall.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Reuse vanilla totem consumption at the delayed lethal resolution, with the original damage source. */
@Mixin(LivingEntity.class)
public interface BackstabFatalAccess {
    @Invoker("checkTotemDeathProtection") boolean riftfall$tryTotem(DamageSource source);
}
