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
    public static final int ATTACK_TICKS=38,RECOVERY_TICKS=94,GREATSWORD_ATTACK_TICKS=50,GREATSWORD_RECOVERY_TICKS=102,GREATSWORD_DEATH_TICKS=40,DEATH_TICKS=38,GETUP_TICKS=54;
    private static final ResourceLocation RESTRAINT=ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"backstab");
    private static final Map<UUID,Victim> VICTIMS=new HashMap<>();
    private static final class Victim {
        final LivingEntity target,attacker;final yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<AttackAnimation> attack;final Vec3 anchor;final ResourceLocation dimension;final boolean oldNoAi;
        final float facingYaw;final boolean great;final int attackTicks;final long releaseAt,attackerUntil;
        long until,getupAt=Long.MAX_VALUE;boolean fallen,detached,attackerAborted;DamageSource fatalSource;
        Victim(LivingEntity target,LivingEntity attacker){
            this.target=target;this.attacker=attacker;attack=animation(attacker);anchor=target.position();dimension=target.level().dimension().location();
            facingYaw=target.getYRot();great=attack==GoblinAnimations.GREATSWORD_BACKSTAB;attackTicks=great?GREATSWORD_ATTACK_TICKS:ATTACK_TICKS;
            releaseAt=target.level().getGameTime()+31;attackerUntil=target.level().getGameTime()+attackTicks;
            oldNoAi=target instanceof Mob mob && mob.isNoAi();until=target.level().getGameTime()+attackTicks+4;
        }
    }
    static boolean greatsword(yesman.epicfight.world.capabilities.item.WeaponCategory category){
        return category==yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.GREATSWORD;
    }
    static yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<AttackAnimation> animation(LivingEntity attacker){
        return greatsword(EpicFightCapabilities.getItemStackCapability(attacker.getMainHandItem()).getWeaponCategory())
            ?GoblinAnimations.GREATSWORD_BACKSTAB:GoblinAnimations.SHIELD_BACKSTAB;
    }
    static double maximumDistance(LivingEntity attacker){return animation(attacker)==GoblinAnimations.GREATSWORD_BACKSTAB?2.2:1.5;}
    static void clientCorpseHold(LivingEntity entity,int ticks,boolean great){
        entity.getPersistentData().putLong("RiftfallBackstabCorpsePoseUntil",entity.level().getGameTime()+ticks);
        entity.getPersistentData().putBoolean("RiftfallBackstabGreatCorpse",great);
        if(ticks>0)corpseClientPose(entity);
    }
    static void clientVictimView(Player player,boolean active,float yaw){
        player.getPersistentData().putBoolean("RiftfallBackstabVictimModel",active);
        if(active)player.getPersistentData().putFloat("RiftfallBackstabVictimFacing",yaw);
        victimClientPose(player);
    }
    private static void victimClientPose(LivingEntity entity){
        if(!(entity instanceof Player player))return;
        var p=EpicFightCapabilities.getEntityPatch(player,yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.class);if(p==null)return;
        var d=player.getPersistentData();
        if(d.getBoolean("RiftfallBackstabVictimModel") || holdsCorpse(player)){
            p.setModelYRot(d.getFloat("RiftfallBackstabVictimFacing"),false);d.putBoolean("RiftfallBackstabVictimModelOwned",true);
        }else if(d.getBoolean("RiftfallBackstabVictimModelOwned")){p.disableModelYRot(false);d.remove("RiftfallBackstabVictimModelOwned");}
    }
    private static void corpseClientPose(LivingEntity entity){
        if(!holdsCorpse(entity))return;
        entity.deathTime=0;var p=patch(entity);if(p==null)return;
        var clip=entity.getPersistentData().getBoolean("RiftfallBackstabGreatCorpse")?GoblinAnimations.GREATSWORD_BACKSTAB_DEATH:GoblinAnimations.BACKSTAB_DEATH;
        if(p.getAnimator().getPlayerFor(null).getRealAnimation().get()!=clip.get())p.getAnimator().playAnimationInstantly(clip);
        p.getAnimator().getPlayerFor(null).setElapsedTime(Math.max(0,clip.get().getTotalTime()-.001F));
    }
    public static boolean holdsCorpse(LivingEntity entity){return entity.level().isClientSide && entity.getPersistentData().getLong("RiftfallBackstabCorpsePoseUntil")>entity.level().getGameTime();}
    public static boolean captured(LivingEntity entity){return VICTIMS.containsKey(entity.getUUID()) || BackstabActorLock.active(entity);}
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
        return offset.horizontalDistanceSqr()<Math.pow(maximumDistance(attacker),2) && Math.abs(offset.y)<.6 && GoblinGrapple.isBehind(target.getLookAngle(),offset)
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
        if(attacker instanceof GoblinEntity goblin)GoblinBuckler.lowerForAttack(goblin,state.attackTicks);
        BackstabActorLock.begin(attacker,yaw,state.attackTicks,()->state.attackerAborted=true);
        if(target instanceof ServerPlayer player)BackstabViewPayload.send(player,true,state.facingYaw);
        patch(attacker).playAnimationSynchronized(state.attack,0);
        attacker.getPersistentData().putInt("RiftfallBackstabAttempts",attacker.getPersistentData().getInt("RiftfallBackstabAttempts")+1);
        return new MobActiveSkill.Execution(){
            @Override public boolean running(){return attacker.isAlive() && target.isAlive() && attacker.level()==target.level()
                && !attacker.isRemoved() && !patch(attacker).isStunned() && attacker.level().getGameTime()-began<state.attackTicks;}
            @Override public void tick(){attacker.setSprinting(false);if(attacker instanceof Mob mob)mob.getNavigation().stop();}
            @Override public void stop(){if(VICTIMS.get(target.getUUID())==state && !state.fallen)release(state,true);attacker.setSprinting(false);}
        };
    }
    static KnockdownAnimation recovery(yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<? extends KnockdownAnimation> accessor){
        var animation=new KnockdownAnimation(.08F,accessor,yesman.epicfight.gameasset.Armatures.BIPED);
        animation.addStateRemoveOld(EntityState.CAN_USE_ITEM,false);animation.addStateRemoveOld(EntityState.CAN_SWITCH_HAND_ITEM,false);
        float getup=accessor==GoblinAnimations.BACKSTAB_FALL?2F:accessor==GoblinAnimations.GREATSWORD_BACKSTAB_FALL?2.4F:
            accessor==GoblinAnimations.BACKSTAB_GETUP?0:Float.MAX_VALUE;
        animation.removeState(EntityState.ATTACK_RESULT);animation.removeState(EntityState.KNOCKDOWN);
        if(getup>0){
            animation.newTimePair(0,getup);
            animation.addState(EntityState.ATTACK_RESULT,(java.util.function.Function<DamageSource,AttackResult.ResultType>)source->
                source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)?AttackResult.ResultType.SUCCESS:AttackResult.ResultType.MISSED);
            animation.addState(EntityState.KNOCKDOWN,true);
        }
        if(getup<Float.MAX_VALUE){
            animation.newTimePair(getup,Float.MAX_VALUE);
            animation.addState(EntityState.ATTACK_RESULT,(java.util.function.Function<DamageSource,AttackResult.ResultType>)source->AttackResult.ResultType.SUCCESS);
            animation.addState(EntityState.KNOCKDOWN,false);
        }
        return animation;
    }
    private static boolean ownHit(Victim state,DamageSource source){
        return source instanceof EpicFightDamageSource epic && epic.getEntity()==state.attacker && epic.getAnimation()!=null && epic.getAnimation().get()==state.attack.get();
    }
    private static void fall(Victim state,DamageSource fatal){
        long now=state.target.level().getGameTime();state.fallen=true;state.fatalSource=fatal;
        state.getupAt=fatal!=null?Long.MAX_VALUE:now+BackstabTiming.getupDelayTicks(state.great);
        state.until=fatal!=null?BackstabTiming.fatalResolution(state.attackerUntil,now,state.great?GREATSWORD_DEATH_TICKS:DEATH_TICKS)
            :now+(state.great?GREATSWORD_RECOVERY_TICKS:RECOVERY_TICKS);
        var clip=fatal!=null?(state.great?GoblinAnimations.GREATSWORD_BACKSTAB_DEATH:GoblinAnimations.BACKSTAB_DEATH)
            :(state.great?GoblinAnimations.GREATSWORD_BACKSTAB_FALL:GoblinAnimations.BACKSTAB_FALL);
        patch(state.target).playAnimationSynchronized(clip,0);
        var data=state.target.getPersistentData();data.putBoolean("RiftfallBackstabFatalPending",fatal!=null);data.putBoolean("RiftfallBackstabProtected",true);data.putLong("RiftfallBackstabRecoveryUntil",state.until);
        state.attacker.getPersistentData().putInt("RiftfallBackstabHits",state.attacker.getPersistentData().getInt("RiftfallBackstabHits")+1);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void deferFatal(LivingDamageEvent.Pre event){
        var state=VICTIMS.get(event.getEntity().getUUID());
        if(state==null || state.fallen || !ownHit(state,event.getSource()) || event.getNewDamage()<=0 || event.getNewDamage()<state.target.getHealth())return;
        // Armor/absorption have already been accounted for. Never restore lost health or replay mitigation.
        event.setNewDamage(BackstabTiming.initialLethalDamage(state.target.getHealth()));fall(state,event.getSource());
    }
    @SubscribeEvent public static void landed(LivingDamageEvent.Post event){
        var state=VICTIMS.get(event.getEntity().getUUID());if(state==null || event.getNewDamage()<=0)return;
        if(state.fallen){
            if(recovering(state)){
                release(state,false);
                if(state.target.isAlive()){
                    var targetPatch=patch(state.target);
                    if(targetPatch!=null)targetPatch.applyStun(yesman.epicfight.world.damagesource.StunType.SHORT,.45F);
                    state.target.getPersistentData().putLong("RiftfallBackstabRecoveryInterrupted",state.target.level().getGameTime());
                }
            }
            return;
        }
        if(ownHit(state,event.getSource()) && state.target.isAlive())fall(state,null);else release(state,true);
    }
    private static void finishFatal(Victim state){
        var source=state.fatalSource;if(source==null)return;state.fatalSource=null;
        state.target.getPersistentData().putBoolean("RiftfallBackstabFatalPending",false);
        if(((org.Azkerash.riftfall.mixin.BackstabFatalAccess)state.target).riftfall$tryTotem(source)){
            state.until=state.target.level().getGameTime()+GETUP_TICKS;state.detached=true;state.getupAt=state.target.level().getGameTime()+2;
            patch(state.target).playAnimationSynchronized(GoblinAnimations.BACKSTAB_GETUP,0);
            state.target.getPersistentData().putLong("RiftfallBackstabRecoveryUntil",state.until);return;
        }
        release(state,false);
        state.target.getCombatTracker().recordDamage(source,state.target.getHealth());
        state.target.setHealth(0);state.target.die(source);
        if(!state.target.isAlive())BackstabCorpsePayload.send(state.target,22,state.great);
    }
    private static void resolveAndRelease(Victim state){
        if(state.fatalSource!=null && state.target.isAlive())finishFatal(state);
        if(VICTIMS.get(state.target.getUUID())==state)release(state,false);
    }
    private static boolean recovering(Victim state){return BackstabTiming.vulnerable(state.fallen,state.fatalSource!=null,state.target.level().getGameTime(),state.getupAt);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void protect(LivingIncomingDamageEvent event){
        var state=VICTIMS.get(event.getEntity().getUUID());
        if(state!=null && state.fallen && !recovering(state) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hold(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof LivingEntity target))return;
        if(target.level().isClientSide){corpseClientPose(target);victimClientPose(target);return;}
        var state=VICTIMS.get(target.getUUID());if(state==null)return;
        boolean attackerInterrupted=state.attackerAborted || !state.attacker.isAlive() || state.attacker.isRemoved() || patch(state.attacker)==null || patch(state.attacker).isStunned();
        if(state.great && state.fallen && !state.detached && target.level().getGameTime()<state.releaseAt && attackerInterrupted){
            state.detached=true;state.until=target.level().getGameTime()+(state.fatalSource!=null?DEATH_TICKS:RECOVERY_TICKS);
            state.getupAt=state.fatalSource!=null?Long.MAX_VALUE:target.level().getGameTime()+BackstabTiming.getupDelayTicks(false);
            patch(target).playAnimationSynchronized(state.fatalSource!=null?GoblinAnimations.BACKSTAB_DEATH:GoblinAnimations.BACKSTAB_FALL,0);
            target.getPersistentData().putLong("RiftfallBackstabRecoveryUntil",state.until);
        }
        if(target.level().getGameTime()>=state.until && state.fatalSource!=null && target.isAlive()){finishFatal(state);return;}
        boolean invalid=!target.isAlive() || target instanceof ServerPlayer p && (p.hasDisconnected() || p.isCreative() || p.isSpectator())
            || target.isPassenger() || target.isInWaterOrBubble() || target.isInLava() || !supported(target,state.anchor) || !target.level().dimension().location().equals(state.dimension)
            || target.position().distanceToSqr(state.anchor)>4 || target.level().getGameTime()>=state.until
            || !state.fallen && attackerInterrupted;
        if(invalid){if(state.fatalSource!=null && target.isAlive())resolveAndRelease(state);else release(state,true);return;}
        if(state.fallen)target.getPersistentData().putBoolean("RiftfallBackstabProtected",!recovering(state));
        float cameraYaw=target.getYRot(),cameraPitch=target.getXRot();
        target.setDeltaMovement(0,target.getDeltaMovement().y,0);target.setSprinting(false);
        if(target.position().distanceToSqr(state.anchor)>.0001){
            if(target instanceof ServerPlayer player)player.connection.teleport(state.anchor.x,state.anchor.y,state.anchor.z,cameraYaw,cameraPitch);
            else target.setPos(state.anchor);
        }
        target.setYRot(state.facingYaw);target.setYBodyRot(state.facingYaw);target.setYHeadRot(state.facingYaw);
        if(target instanceof Player){var pp=EpicFightCapabilities.getEntityPatch(target,yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.class);if(pp!=null)pp.setModelYRot(state.facingYaw,false);}
    }
    private static void release(Victim state,boolean resetAnimation){
        VICTIMS.remove(state.target.getUUID(),state);restrain(state.target,false);
        if(state.target instanceof ServerPlayer p){BackstabViewPayload.send(p,false);var pp=EpicFightCapabilities.getEntityPatch(p,yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.class);if(pp!=null)pp.disableModelYRot(false);}
        if(state.target instanceof Mob mob)mob.setNoAi(state.oldNoAi);
        var data=state.target.getPersistentData();data.putBoolean("RiftfallBackstabLocked",false);data.putBoolean("RiftfallBackstabProtected",false);data.remove("RiftfallBackstabRecoveryUntil");data.remove("RiftfallBackstabFatalPending");
        if(resetAnimation && state.target.isAlive()){
            var targetPatch=patch(state.target);
            if(targetPatch!=null && (targetPatch.getAnimator().getPlayerFor(null).getAnimation().get()==GoblinAnimations.BACKSTAB_CAUGHT.get()
                || targetPatch.getAnimator().getPlayerFor(null).getAnimation().get()==GoblinAnimations.BACKSTAB_FALL.get()
                || targetPatch.getAnimator().getPlayerFor(null).getAnimation().get()==GoblinAnimations.GREATSWORD_BACKSTAB_FALL.get()
                || targetPatch.getAnimator().getPlayerFor(null).getAnimation().get()==GoblinAnimations.BACKSTAB_GETUP.get()))
                targetPatch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE,0);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){var state=VICTIMS.get(event.getEntity().getUUID());if(state!=null)resolveAndRelease(state);}
    @SubscribeEvent public static void unloaded(EntityLeaveLevelEvent event){var state=VICTIMS.get(event.getEntity().getUUID());if(state!=null)resolveAndRelease(state);}
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event){for(var state:List.copyOf(VICTIMS.values()))resolveAndRelease(state);}
    private BackstabSequence(){}
}
