package org.Azkerash.riftfall.stamina;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.Azkerash.riftfall.Riftfall;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** No world scans: lifecycle, launch, and damage hooks cover vanilla and modded Mob subclasses. */
@EventBusSubscriber(modid=Riftfall.MODID)
public final class MobStaminaEvents {
    private static final String PROJECTILE_PAID="RiftfallStaminaPaidProjectile";
    @SubscribeEvent public static void pre(EntityTickEvent.Pre event){
        if(event.getEntity() instanceof Mob mob && !mob.level().isClientSide && MobStamina.uses(mob)){
            var patch=EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);
            boolean working=MobStamina.fastMovement(mob) || mob.isUsingItem() || patch!=null && patch.getEntityState().inaction()
                || mob.getPersistentData().getBoolean("RiftfallBurrowing");
            if(MobStamina.fastMovement(mob) && !MobStamina.sprintAllowed(mob))mob.getNavigation().stop();
            MobStamina.beforeTick(mob,working);
        }
    }
    @SubscribeEvent public static void post(EntityTickEvent.Post event){
        if(event.getEntity() instanceof Mob mob && !mob.level().isClientSide && MobStamina.uses(mob)){
            MobStamina.afterTick(mob);
            if(MobStamina.panting(mob) && !MobStamina.manual(mob)){
                var patch=EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);
                if(patch!=null && !patch.isStunned() && patch.getAnimator().getPlayerFor(null).getAnimation().get() instanceof yesman.epicfight.api.animation.types.AttackAnimation)
                    patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE,0);
            }
        }
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide())return;
        if(event.getEntity() instanceof Mob mob && MobStamina.uses(mob) && !MobStamina.manual(mob)
                && mob.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof Rest))
            mob.goalSelector.addGoal(-1,new Rest(mob));
        if(event.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Mob mob){
            if(!MobStamina.automaticContact(mob,"bow")){event.setCanceled(true);return;}
            projectile.getPersistentData().putBoolean(PROJECTILE_PAID,true);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void customDamage(LivingIncomingDamageEvent event){
        if(!(event.getSource().getEntity() instanceof Mob mob) || !MobStamina.uses(mob))return;
        var direct=event.getSource().getDirectEntity();
        // An already-fired projectile keeps working if the shooter subsequently becomes exhausted.
        if(direct!=null && direct!=mob){
            if(direct.getPersistentData().getBoolean(PROJECTILE_PAID))return;
            if(!MobStamina.automaticContact(mob,"bow")){event.setCanceled(true);return;}
            direct.getPersistentData().putBoolean(PROJECTILE_PAID,true);return;
        }
        if(!MobStamina.automaticContact(mob,"swipe"))event.setCanceled(true);
    }
    private static final class Rest extends Goal {
        private final Mob mob;
        Rest(Mob mob){this.mob=mob;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.JUMP,Flag.LOOK));}
        @Override public boolean canUse(){return MobStamina.panting(mob);}
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){tick();}
        @Override public void tick(){mob.getNavigation().stop();mob.setSprinting(false);mob.stopUsingItem();}
    }
    private MobStaminaEvents(){}
}
