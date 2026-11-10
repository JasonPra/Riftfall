package org.Azkerash.riftfall.mixin;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(Camera.class)
public interface CameraAccess {
    @Invoker("setPosition") void riftfall$setPosition(Vec3 position);
    @Accessor("eyeHeight") float riftfall$eyeHeight();
    @Accessor("eyeHeightOld") float riftfall$eyeHeightOld();
}
