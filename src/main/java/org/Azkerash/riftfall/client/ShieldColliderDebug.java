package org.Azkerash.riftfall.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.shield.ShieldCollisions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Magenta boxes are the physical shield volumes; F8's cyan boxes remain model alignment bounds. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class ShieldColliderDebug {
    @SubscribeEvent public static void draw(RenderLevelStageEvent event) {
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES || mc.level==null || mc.player==null || !mc.getEntityRenderDispatcher().shouldRenderHitBoxes())return;
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack().last();var buffers=mc.renderBuffers().bufferSource();var consumer=buffers.getBuffer(RenderType.lines());
        for(var entity:mc.level.getEntitiesOfClass(LivingEntity.class,mc.player.getBoundingBox().inflate(32))) {
            var held=ShieldCollisions.held(entity);if(held==null)continue;
            var corners=ShieldCollisions.box(held,1).corners();
            for(int i=0;i<8;i++)for(int bit=1;bit<=4;bit*=2)if((i&bit)==0) {
                var a=corners[i].subtract(camera);var b=corners[i|bit].subtract(camera);var normal=b.subtract(a).normalize();
                consumer.addVertex(pose.pose(),(float)a.x,(float)a.y,(float)a.z).setColor(255,80,255,255).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
                consumer.addVertex(pose.pose(),(float)b.x,(float)b.y,(float)b.z).setColor(255,80,255,255).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
            }
        }
    }
    private ShieldColliderDebug(){}
}
