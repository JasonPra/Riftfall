package org.Azkerash.riftfall;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Locks world movement and the animated model, never the player's camera orientation. */
public final class BackstabActorLock {
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"backstab_actor");
    private record Lock(ServerPlayer player,Vec3 anchor,ResourceLocation dimension,float yaw,long until,Runnable aborted){}
    private static final Map<UUID,Lock> ACTIVE=new HashMap<>();
    static boolean active(LivingEntity entity){return ACTIVE.containsKey(entity.getUUID());}
    static void begin(LivingEntity entity,float yaw,int ticks,Runnable aborted){
        if(!(entity instanceof ServerPlayer p))return;
        ACTIVE.put(p.getUUID(),new Lock(p,p.position(),p.level().dimension().location(),yaw,p.level().getGameTime()+ticks,aborted));
        restrain(p,true);p.setSprinting(false);p.getPersistentData().putBoolean("RiftfallBackstabAttacking",true);
        p.getPersistentData().putFloat("RiftfallBackstabFacing",yaw);BackstabActorPayload.send(p,ticks,yaw);
    }
    private static void restrain(Player p,boolean on){
        for(var type:List.of(Attributes.MOVEMENT_SPEED,Attributes.JUMP_STRENGTH)){
            var a=p.getAttribute(type);if(a==null)continue;a.removeModifier(ID);
            if(on)a.addTransientModifier(new AttributeModifier(ID,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
    static void finish(LivingEntity entity,boolean interrupted){
        var lock=ACTIVE.remove(entity.getUUID());if(lock==null)return;
        if(interrupted)lock.aborted.run();restrain(lock.player,false);
        lock.player.getPersistentData().putBoolean("RiftfallBackstabAttacking",false);
        var patch=EpicFightCapabilities.getEntityPatch(entity,PlayerPatch.class);if(patch!=null)patch.disableModelYRot(false);
        if(!entity.isRemoved())BackstabActorPayload.send(lock.player,0,lock.yaw);
    }
    public static boolean clientLocked(Player p){return p.getPersistentData().getLong("RiftfallBackstabActorUntil")>p.level().getGameTime();}
    static void receive(Player p,int ticks,float yaw){
        p.getPersistentData().putLong("RiftfallBackstabActorUntil",p.level().getGameTime()+ticks);
        p.getPersistentData().putFloat("RiftfallBackstabActorYaw",yaw);clientPose(p);
    }
    private static void clientPose(Player p){
        var patch=EpicFightCapabilities.getEntityPatch(p,PlayerPatch.class);if(patch==null)return;
        var data=p.getPersistentData();
        if(clientLocked(p)){patch.setModelYRot(data.getFloat("RiftfallBackstabActorYaw"),false);data.putBoolean("RiftfallBackstabModelLock",true);}
        else if(data.getBoolean("RiftfallBackstabModelLock")){patch.disableModelYRot(false);data.remove("RiftfallBackstabModelLock");}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof Player p))return;
        if(p.level().isClientSide){clientPose(p);return;}
        var lock=ACTIVE.get(p.getUUID());if(lock==null)return;
        var patch=EpicFightCapabilities.getEntityPatch(p,PlayerPatch.class);
        if(!p.isAlive() || p.isRemoved() || ((ServerPlayer)p).hasDisconnected() || p.isPassenger()
            || !p.level().dimension().location().equals(lock.dimension) || p.position().distanceToSqr(lock.anchor)>16 || patch==null || patch.isStunned()){
            finish(p,true);return;
        }
        if(p.level().getGameTime()>=lock.until){finish(p,false);return;}
        float cameraYaw=p.getYRot(),cameraPitch=p.getXRot();
        p.getPersistentData().putFloat("RiftfallBackstabCameraYaw",cameraYaw);
        if(p.position().distanceToSqr(lock.anchor)>.0001)((ServerPlayer)p).connection.teleport(lock.anchor.x,lock.anchor.y,lock.anchor.z,cameraYaw,cameraPitch);
        p.setDeltaMovement(Vec3.ZERO);p.setSprinting(false);p.setYRot(lock.yaw);p.setYBodyRot(lock.yaw);p.setYHeadRot(lock.yaw);patch.setModelYRot(lock.yaw,false);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){finish(event.getEntity(),true);}
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e){for(var l:List.copyOf(ACTIVE.values()))finish(l.player,true);}
    private BackstabActorLock(){}
}
