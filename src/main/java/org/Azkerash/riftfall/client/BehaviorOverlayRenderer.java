package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.util.TriState;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.behavior.BehaviorOverlayPayload;

/** Uses the nameplate hook also posted by Epic Fight, preserving its renderer's coordinate/buffer setup. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class BehaviorOverlayRenderer {
    private static List<BehaviorOverlayPayload.Entry> entries=List.of();
    private static long receivedAt;
    public static void accept(BehaviorOverlayPayload frame) {
        var level=Minecraft.getInstance().level;
        entries=level==null?List.of():frame.entries();receivedAt=level==null?0:level.getGameTime();
    }
    @SubscribeEvent public static void disconnected(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level==null) entries=List.of();
    }
    @SubscribeEvent public static void render(RenderNameTagEvent event) {
        var mc=Minecraft.getInstance();var entity=event.getEntity();
        if (mc.level==null || mc.player==null || mc.level.getGameTime()-receivedAt>20 || entries.isEmpty()
                || !entity.isAlive() || entity.distanceToSqr(mc.player)>32*32) return;
        var entry=entries.stream().filter(e->e.entityId()==entity.getId() && e.uuid().equals(entity.getUUID())).findFirst().orElse(null);
        if (entry==null) return;
        // Only suppress this viewer's ordinary nameplate while the debug replacement is present.
        event.setCanRender(TriState.FALSE);
        var poses=event.getPoseStack();poses.pushPose();poses.translate(0,entity.getBbHeight()+.8,0);
        poses.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());poses.scale(.02F,-.02F,.02F);
        int y=-entry.lines().size()*10;
        for (String line:entry.lines()) {
            mc.font.drawInBatch(line,-mc.font.width(line)/2F,y,0xFFFFFFFF,false,poses.last().pose(),event.getMultiBufferSource(),Font.DisplayMode.SEE_THROUGH,0x60000000,LightTexture.FULL_BRIGHT);
            y+=10;
        }
        poses.popPose();
    }
    private BehaviorOverlayRenderer() {}
}
