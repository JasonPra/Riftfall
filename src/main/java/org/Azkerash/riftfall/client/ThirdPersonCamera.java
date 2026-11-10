package org.Azkerash.riftfall.client;
import net.minecraft.client.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.Azkerash.riftfall.mixin.CameraAccess;
import yesman.epicfight.api.client.event.types.camera.BuildCameraTransform;
/** Position-only camera enhancement; Epic Fight retains camera rotation, lock-on and aiming control. */
public final class ThirdPersonCamera {
    private static final CameraBoom BOOM=new CameraBoom();
    private static long previousFrame,lastTick=Long.MIN_VALUE;
    private static float lastPartial=Float.NaN;
    private static net.minecraft.client.Camera lastCamera;
    private static Vec3 cachedPosition;
    private static float cachedYaw,cachedPitch;
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;
    public static void reset(){BOOM.reset();previousFrame=0;previousLevel=null;lastCamera=null;lastPartial=Float.NaN;lastTick=Long.MIN_VALUE;cachedPosition=null;}
    public static void build(BuildCameraTransform.Post event){
        enhance(event.getCamera(),event.getPartialTick());
    }
    public static void enhance(Camera camera,float partial){
        var mc=Minecraft.getInstance();
        if(mc.level!=null && lastCamera==camera && lastTick==mc.level.getGameTime() && lastPartial==partial && cachedYaw==camera.getYRot() && cachedPitch==camera.getXRot()){
            if(cachedPosition!=null)((CameraAccess)camera).riftfall$setPosition(cachedPosition);return;
        }
        cachedPosition=null;cachedYaw=camera.getYRot();cachedPitch=camera.getXRot();
        lastCamera=camera;lastTick=mc.level==null?Long.MIN_VALUE:mc.level.getGameTime();lastPartial=partial;
        if(mc.level==null || !CameraConfig.ENABLED.get() || mc.options.getCameraType()!=CameraType.THIRD_PERSON_BACK
            || !(camera.getEntity() instanceof Player p) || p.isSpectator() || p.isSleeping() || yesman.epicfight.api.client.camera.EpicFightCameraAPI.getInstance().isZooming()){
            BOOM.reset();previousFrame=0;return;
        }
        if(previousLevel!=mc.level){BOOM.reset();previousLevel=mc.level;}
        var access=(CameraAccess)camera;
        var pivot=p.getPosition(partial).add(0,net.minecraft.util.Mth.lerp(partial,access.riftfall$eyeHeightOld(),access.riftfall$eyeHeight()),0);
        var look=camera.getLookVector();var left=camera.getLeftVector();var up=camera.getUpVector();
        var l=new Vec3(left.x,left.y,left.z);var u=new Vec3(up.x,up.y,up.z);
        var offset=new Vec3(look.x,look.y,look.z).scale(-CameraConfig.DISTANCE.get()).subtract(l.scale(CameraConfig.SHOULDER.get())).add(0,CameraConfig.HEIGHT.get(),0);
        double allowed=offset.length();
        // Center and near-plane corners protect the shoulder offset as well as the boom.
        for(int i=0;i<5;i++){
            var pad=i==0?Vec3.ZERO:l.scale(((i-1)&1)==0?-.14:.14).add(u.scale(((i-1)&2)==0?-.14:.14));
            var start=pivot.add(pad);var hit=mc.level.clip(new ClipContext(start,start.add(offset),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.MISS)allowed=Math.min(allowed,Math.max(0,start.distanceTo(hit.getLocation())-.08));
        }
        long now=System.nanoTime();double dt=previousFrame==0?0:(now-previousFrame)/1E9;previousFrame=now;
        double distance=BOOM.advance(allowed,dt,CameraConfig.RECOVERY.get());
        cachedPosition=pivot.add(offset.normalize().scale(distance));access.riftfall$setPosition(cachedPosition);
    }
    private ThirdPersonCamera(){}
}
