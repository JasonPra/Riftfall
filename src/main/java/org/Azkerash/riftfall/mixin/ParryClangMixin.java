package org.Azkerash.riftfall.mixin;

import net.minecraft.sounds.SoundEvent;
import org.Azkerash.riftfall.shield.GuardClang;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.event.types.entity.TakeDamageEvent;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

@Mixin(value=ParryingSkill.class,remap=false)
public abstract class ParryClangMixin {
    @Shadow private int parryWindow;
    @Redirect(method="guard",at=@At(value="INVOKE",target="Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void riftfall$timedClang(ServerPlayerPatch patch,SoundEvent sound,float minimum,float maximum,
            SkillContainer container,CapabilityItem item,ServerPlayerPatch defender,TakeDamageEvent.Income event,float knockback,float impact,boolean advanced) {
        // Exactly the stock success check, sampled before the skill resets LAST_ACTIVE.
        boolean parry=defender.getOriginal().tickCount-container.getDataManager().getDataValue(EpicFightSkillDataKeys.LAST_ACTIVE)<parryWindow;
        GuardClang.play(patch,sound,minimum,maximum,parry);
    }
}
