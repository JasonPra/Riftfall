package org.Azkerash.riftfall.mixin;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.api.collider.LineCollider;

@Mixin(value=LineCollider.class,remap=false)
public interface ShieldLineAccess {
    @Accessor("worldVec") Vec3 riftfall$vector();
}
