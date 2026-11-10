package org.Azkerash.riftfall.mixin;

import net.minecraft.sounds.SoundEvent;
import org.Azkerash.riftfall.shield.GuardClang;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Also covers Impact Guard and the ordinary-guard fallback from ParryingSkill. */
@Mixin(value=GuardSkill.class,remap=false)
public abstract class GuardClangMixin {
    @Redirect(method="guard",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void riftfall$ordinaryClang(ServerPlayerPatch patch,SoundEvent sound,float minimum,float maximum) {
        GuardClang.play(patch,sound,minimum,maximum,false);
    }
}
