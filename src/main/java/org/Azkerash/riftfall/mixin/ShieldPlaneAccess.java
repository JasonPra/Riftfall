package org.Azkerash.riftfall.mixin;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.collider.PlaneCollider;

@Mixin(value=PlaneCollider.class,remap=false)
public interface ShieldPlaneAccess {
    @Accessor("worldPos") Vec3[] riftfall$vectors();
}
