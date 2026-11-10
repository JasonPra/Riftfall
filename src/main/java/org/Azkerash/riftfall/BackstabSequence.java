package org.Azkerash.riftfall;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.Azkerash.riftfall.behavior.skills.MobActiveSkill;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/** Shared player/NPC critical-hit sequence. A real weapon hit starts the protected fall and recovery. */
public final class BackstabSequence {
    public static final int ATTACK_TICKS=36,RECOVERY_TICKS=94;
    private static final ResourceLocation RESTRAINT=ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"backstab");
    private static final Map<UUID,Victim> VICTIMS=new HashMap<>();
    private static final class Victim {
        final LivingEntity target,attacker;final yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<AttackAnimation> attack;final Vec3 anchor;final ResourceLocation dimension;final boolean oldNoAi;
        long until;boolean fallen;
        Victim(LivingEntity target,LivingEntity attacker){
            this.target=target;this.attacker=attacker;attack=animation(attacker);anchor=target.position();dimension=target.level().dimension().location();
            oldNoAi=target instanceof Mob mob && mob.isNoAi();until=target.level().getGameTime()+ATTACK_TICKS+4;
        }
    }
    static boolean greatsword(yesman.epicfight.world.capabilities.item.WeaponCategory category){
        return category==yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.GREATSWORD;
    }
    static yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<AttackAnimation> animation(LivingEntity attacker){
        return greatsword(EpicFightCapabilities.getItemStackCapability(attacker.getMainHandItem()).getWeaponCategory())
            ?GoblinAnimations.GREATSWORD_BACKSTAB:GoblinAnimations.SHIELD_BACKSTAB;
    }
    public static boolean captured(LivingEntity entity){return VICTIMS.containsKey(entity.getUUID());}
    private static LivingEntityPatch<?> patch(LivingEntity entity){return EpicFightCapabilities.getEntityPatch(entity,LivingEntityPatch.class);}
    private static boolean proneSpace(LivingEntity target){
        var forward=Vec3.directionFromRotation(0,target.getYRot());
        for(int i=0;i<4;i++){
            var p=target.position().add(forward.scale(i*.4));
            if(!target.level().noCollision(target,new AABB(p.x-.38,p.y+.04,p.z-.38,p.x+.38,p.y+.7,p.z+.38)))return false;
            var below=net.minecraft.core.BlockPos.containing(p.x,p.y-.05,p.z);
            var shape=target.level().getBlockState(below).getCollisionShape(target.level(),below);
            if(shape.isEmpty() || Math.abs(p.y-below.getY()-shape.max(net.minecraft.core.Direction.Axis.Y))>.1)return false;
        }
        return true;
    }
    private static boolean humanoid(LivingEntity entity){
        var patch=patch(entity);if(patch==null)return false;
        for(var joint:List.of("Torso","Head","Arm_R","Hand_R","Thigh_R","Leg_R"))
            if(patch.getArmature().searchJointByName(joint)==null)return false;
        return true;
    }
    private static boolean supported(LivingEntity entity,Vec3 point){
        var below=net.minecraft.core.BlockPos.containing(point.x,point.y-.05,point.z);
        var shape=entity.level().getBlockState(below).getCollisionShape(entity.level(),below);
        return !shape.isEmpty() && Math.abs(point.y-below.getY()-shape.max(net.minecraft.core.Direction.Axis.Y))<=.1;
    }
    public static boolean canStart(LivingEntity attacker,LivingEntity target){
        if(target==null || attacker==target || !attacker.isAlive() || !target.isAlive() || attacker.isAlliedTo(target)
            || target instanceof Player player && (player.isCreative() || player.isSpectator())
            || target instanceof ServerPlayer player && GoblinGrapple.restrained(player)
            || captured(attacker) || captured(target) || attacker.getMainHandItem().isEmpty()
            || !(EpicFightCapabilities.getItemStackCapability(attacker.getMainHandItem()) instanceof yesman.epicfight.world.capabilities.item.WeaponCapability)
            || attacker.getMainHandItem().getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem
            || attacker.isPassenger() || target.isPassenger() || target.isInWaterOrBubble() || target.isInLava() || !attacker.onGround() || !target.onGround()
            || attacker.level()!=target.level() || !humanoid(attacker) || !humanoid(target)
            || attacker.level().getGameTime()<attacker.getPersistentData().getLong("RiftfallBackstabReady"))return false;
        var offset=attacker.position().subtract(target.position());
        return offset.horizontalDistanceSqr()<1.5*1.5 && Math.abs(offset.y)<.6 && GoblinGrapple.isBehind(target.getLookAngle(),offset)
            && attacker.hasLineOfSight(target) && proneSpace(target)
            && (attacker.level().isClientSide || WeaponReach.canContact(attacker,target,animation(attacker).get()));
    }
    private static void restrain(LivingEntity target,boolean enabled){
        for(var type:List.of(Attributes.MOVEMENT_SPEED,Attributes.JUMP_STRENGTH)){
            var attribute=target.getAttribute(type);if(attribute==null)continue;attribute.removeModifier(RESTRAINT);
            if(enabled)attribute.addTransientModifier(new AttributeModifier(RESTRAINT,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
    public static MobActiveSkill.Execution begin(LivingEntity attacker,LivingEntity target){
        if(!canStart(attacker,target))return null;
        long began=attacker.level().getGameTime();var state=new Victim(target,attacker);
        VICTIMS.put(target.getUUID(),state);restrain(target,true);
        if(target instanceof Mob mob)mob.setNoAi(true);
        target.stopUsingItem();target.setSprinting(false);attacker.setSprinting(false);
        patch(target).playAnimationSynchronized(GoblinAnimations.BACKSTAB_CAUGHT,0);
        target.getPersistentData().putBoolean("RiftfallBackstabLocked",true);
        attacker.getPersistentData().putLong("RiftfallBackstabReady",began+200);
        float yaw=GoblinTactics.staffFacing(target.getX()-attacker.getX(),target.getZ()-attacker.getZ());attacker.setYRot(yaw);attacker.setYBodyRot(yaw);attacker.setYHeadRot(yaw);
        if(attacker instanceof Mob mob)mob.getNavigation().stop();
        if(attacker instanceof GoblinEntity goblin)GoblinBuckler.lowerForAttack(goblin,ATTACK_TICKS);
        patch(attacker).playAnimationSynchronized(state.attack,0);
        attacker.getPersistentData().putInt("RiftfallBackstabAttempts",attacker.getPersistentData().getInt("RiftfallBackstabAttempts")+1);
        return new MobActiveSkill.Execution(){
            @Override public boolean running(){return attacker.isAlive() && target.isAlive() && attacker.level()==target.level()
                && !attacker.isRemoved() && !patch(attacker).isStunned() && attacker.level().getGameTime()-began<ATTACK_TICKS;}
            @Override public void tick(){attacker.setSprinting(false);if(attacker instanceof Mob mob)mob.getNavigation().stop();}
            @Override public void stop(){if(VICTIMS.get(target.getUUID())==state && !state.fallen)release(state,true);attacker.setSprinting(false);}
        };
    }
    static KnockdownAnimation recovery(yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<? extends KnockdownAnimation> accessor){
        var animation=new KnockdownAnimation(.08F,accessor,yesman.epicfight.gameasset.Armatures.BIPED);
        animation.addStateRemoveOld(EntityState.ATTACK_RESULT,(java.util.function.Function<DamageSource,AttackResult.ResultType>)source->
            source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)?AttackResult.ResultType.SUCCESS:AttackResult.ResultType.MISSED);
        animation.addStateRemoveOld(EntityState.CAN_USE_ITEM,false);animation.addStateRemoveOld(EntityState.CAN_SWITCH_HAND_ITEM,false);
        return animation;
    }
    @SubscribeEvent public static void landed(LivingDamageEvent.Post event){
        var state=VICTIMS.get(event.getEntity().getUUID());if(state==null || state.fallen || event.getNewDamage()<=0)return;
        if(!state.target.isAlive()){release(state,false);return;}
        if(event.getSource() instanceof EpicFightDamageSource source && source.getEntity()==state.attacker
            && source.getAnimation()!=null && source.getAnimation().get()==state.attack.get()){
            state.fallen=true;state.until=state.target.level().getGameTime()+RECOVERY_TICKS;
            patch(state.target).playAnimationSynchronized(GoblinAnimations.BACKSTAB_FALL,0);
            state.target.getPersistentData().putBoolean("RiftfallBackstabProtected",true);
            state.target.getPersistentData().putLong("RiftfallBackstabRecoveryUntil",state.until);
            state.attacker.getPersistentData().putInt("RiftfallBackstabHits",state.attacker.getPersistentData().getInt("RiftfallBackstabHits")+1);
        }else release(state,true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void protect(LivingIncomingDamageEvent event){
        var state=VICTIMS.get(event.getEntity().getUUID());
        if(state!=null && state.fallen && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))event.setCanceled(true);
    }
    @SubscribeEvent public static void hold(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide)return;
        var state=VICTIMS.get(target.getUUID());if(state==null)return;
        boolean invalid=!target.isAlive() || target instanceof ServerPlayer p && (p.hasDisconnected() || p.isCreative() || p.isSpectator())
            || target.isPassenger() || target.isInWaterOrBubble() || target.isInLava() || !supported(target,state.anchor) || !target.level().dimension().location().equals(state.dimension)
            || target.position().distanceToSqr(state.anchor)>4 || target.level().getGameTime()>=state.until
            || !state.fallen && (!state.attacker.isAlive() || state.attacker.isRemoved() || patch(state.attacker)==null || patch(state.attacker).isStunned());
        if(invalid){release(state,true);return;}
        target.setDeltaMovement(0,target.getDeltaMovement().y,0);target.setSprinting(false);
        if(target.position().distanceToSqr(state.anchor)>.0001){
            if(target instanceof ServerPlayer player)player.connection.teleport(state.anchor.x,state.anchor.y,state.anchor.z,player.getYRot(),player.getXRot());
            else target.setPos(state.anchor);
        }
    }
    private static void release(Victim state,boolean resetAnimation){
        VICTIMS.remove(state.target.getUUID(),state);restrain(state.target,false);
        if(state.target instanceof Mob mob)mob.setNoAi(state.oldNoAi);
        var data=state.target.getPersistentData();data.putBoolean("RiftfallBackstabLocked",false);data.putBoolean("RiftfallBackstabProtected",false);data.remove("RiftfallBackstabRecoveryUntil");
        if(resetAnimation && state.target.isAlive()){
            var targetPatch=patch(state.target);
            if(targetPatch!=null && (targetPatch.getAnimator().getPlayerFor(null).getAnimation().get()==GoblinAnimations.BACKSTAB_CAUGHT.get()
                || targetPatch.getAnimator().getPlayerFor(null).getAnimation().get()==GoblinAnimations.BACKSTAB_FALL.get()))
                targetPatch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE,0);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){var state=VICTIMS.get(event.getEntity().getUUID());if(state!=null)release(state,false);}
    @SubscribeEvent public static void unloaded(EntityLeaveLevelEvent event){var state=VICTIMS.get(event.getEntity().getUUID());if(state!=null)release(state,false);}
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event){for(var state:List.copyOf(VICTIMS.values()))release(state,false);}
    private BackstabSequence(){}
}
