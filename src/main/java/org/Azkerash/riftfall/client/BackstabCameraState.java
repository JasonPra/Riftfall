package org.Azkerash.riftfall.client;
import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.Azkerash.riftfall.*;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
/** Temporary perspective and camera decoupling; never reacquires lock-on when a scene ends. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class BackstabCameraState {
    private static boolean victim,forced,decoupled,previousCoupling,wasTPS;
    private static net.minecraft.world.entity.player.Player scenePlayer;
    private static float orbitYaw,orbitPitch;
    private static net.minecraft.client.multiplayer.ClientLevel observedLevel;
    private static CameraType previous;
    private static long restoreAt;
    public static void victim(boolean active){
        var mc=Minecraft.getInstance();victim=active;
        if(active){
            if(!forced && mc.options.getCameraType().isFirstPerson()){previous=mc.options.getCameraType();forced=true;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        }else restoreAt=mc.level==null?0:mc.level.getGameTime()+2;
    }
    public static boolean freeCamera(){var mc=Minecraft.getInstance();return victim || forced || mc.player!=null && BackstabActorLock.clientLocked(mc.player);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(observedLevel!=null && mc.level!=null && observedLevel!=mc.level){
            if(forced && previous!=null)mc.options.setCameraType(previous);
            victim=false;forced=false;previous=null;ThirdPersonCamera.reset();
        }
        observedLevel=mc.level;
        if(mc.player==null || mc.level==null){
            if(forced && previous!=null)mc.options.setCameraType(previous);
            victim=false;forced=false;previous=null;scenePlayer=null;wasTPS=false;ThirdPersonCamera.reset();
            if(decoupled){EpicFightCameraAPI.getInstance().setCouplingState(previousCoupling);decoupled=false;}return;
        }
        if(victim && forced && mc.options.getCameraType().isFirstPerson())mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if(!victim && forced && mc.player.isAlive() && mc.level.getGameTime()>=restoreAt){
            if(mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK){
                if(mc.player==scenePlayer){mc.player.setYRot(orbitYaw);mc.player.setXRot(orbitPitch);}
                mc.options.setCameraType(previous);
            }
            forced=false;previous=null;
        }
        var camera=EpicFightCameraAPI.getInstance();
        if(freeCamera()){
            if(!decoupled){
                previousCoupling=camera.getCouplingState();scenePlayer=mc.player;
                if(!wasTPS)camera.setCameraRotations(mc.player.getXRot(),mc.player.getYRot(),true);
                decoupled=true;camera.setLockOn(false);
            }
            camera.setCouplingState(false);orbitYaw=camera.getCameraYRot();orbitPitch=camera.getCameraXRot();
        }else{
            if(decoupled){
                if(!camera.isTPSMode() && mc.player==scenePlayer){mc.player.setYRot(orbitYaw);mc.player.setXRot(orbitPitch);}
                camera.setCouplingState(previousCoupling);decoupled=false;scenePlayer=null;
            }
            wasTPS=camera.isTPSMode();
        }
    }
    private BackstabCameraState(){}
}
