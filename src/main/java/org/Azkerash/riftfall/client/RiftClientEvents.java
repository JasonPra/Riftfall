package org.Azkerash.riftfall.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.Azkerash.riftfall.RiftEntities;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid = Riftfall.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RiftClientEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RiftEntities.RIFT.get(), RiftRenderer::new);
    }
}
