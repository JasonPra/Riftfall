package org.Azkerash.riftfall.mixin;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import yesman.epicfight.api.collider.Collider;

@Mixin(value=Collider.class,remap=false)
public interface ShieldColliderAccess {
    @Accessor("worldCenter") Vec3 riftfall$center();
    @Invoker("getHitboxAABB") AABB riftfall$bounds();
}
