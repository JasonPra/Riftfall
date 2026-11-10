package org.Azkerash.riftfall;

import java.util.Comparator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ShieldItem;
import org.Azkerash.riftfall.behavior.skills.MobActiveSkill;
import yesman.epicfight.skill.*;

/** One registered Epic Fight skill, shared by player identity slots and NPC active loadouts. */
public final class BackstabSkill extends Skill implements MobActiveSkill {
    public BackstabSkill(SkillBuilder<?> builder){super(builder);}
    private LivingEntity target(LivingEntity player){
        return player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(1.6),entity->
            BackstabSequence.canStart(player,entity) && player.getLookAngle().dot(entity.position().subtract(player.position()).normalize())>.35).stream()
            .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }
    @Override public void onInitiate(SkillContainer container,yesman.epicfight.api.event.EntityEventListener listener){
        super.onInitiate(container,listener);
        listener.registerEvent(yesman.epicfight.api.event.EpicFightEventHooks.Player.COMBO_ATTACK,event->{
            var player=event.getPlayerPatch().getOriginal();
            if(container.isDisabled() || container.getStack()<=0 && !player.isCreative())return;
            var victim=target(player);
            if(victim!=null && beginPlayer(container,victim))event.cancel();
        },this);
    }
    @Override public void onRemoved(SkillContainer container){
        container.getExecutor().getEventListener().removeListenersBelongTo(this);super.onRemoved(container);
    }
    private boolean beginPlayer(SkillContainer container,LivingEntity victim){
        if(BackstabSequence.begin(container.getServerExecutor().getOriginal(),victim)==null)return false;
        Skill.setSkillStackSynchronize(container,0);super.executeOnServer(container,new CompoundTag());return true;
    }
    @Override public boolean checkExecuteCondition(SkillContainer container){return target(container.getExecutor().getOriginal())!=null;}
    @Override public void executeOnServer(SkillContainer container,CompoundTag arguments){
        var player=container.getServerExecutor().getOriginal();var victim=target(player);
        if(victim!=null)beginPlayer(container,victim);
    }
    @Override public boolean canUse(Mob owner,LivingEntity target){
        return owner.getOffhandItem().getItem() instanceof ShieldItem && !owner.isNoAi() && BackstabSequence.canStart(owner,target);
    }
    @Override public Execution start(Mob owner,LivingEntity target){return BackstabSequence.begin(owner,target);}
    @Override public ResourceLocation getSkillTexture(){return ResourceLocation.fromNamespaceAndPath("epicfight","textures/gui/skills/identity/revelation.png");}
}
