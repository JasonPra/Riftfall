package org.Azkerash.riftfall.mixin;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.Azkerash.riftfall.client.ThirdPersonCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Vanilla's camera path does not always fire Epic Fight's camera-build event. */
@Mixin(Camera.class)
public abstract class ThirdPersonCameraMixin {
    @Inject(method="setup",at=@At("RETURN"))
    private void riftfall$camera(BlockGetter level,Entity entity,boolean detached,boolean inverse,float partial,CallbackInfo ci){ThirdPersonCamera.enhance((Camera)(Object)this,partial);}
}
