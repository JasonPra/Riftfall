package org.Azkerash.riftfall.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.Azkerash.riftfall.RiftEntities;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid = Riftfall.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RiftClientEvents {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent setup) {
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(event -> {
            for (var type : java.util.List.of(RiftEntities.GOBLIN.get(), RiftEntities.GOBLIN_SHAMAN.get()))
                event.addPatchedEntityRenderer(type, t -> new yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer<>(
                    yesman.epicfight.api.client.model.Meshes.BIPED,event.getContext(),t));
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RiftEntities.RIFT.get(), RiftRenderer::new);
        event.registerEntityRenderer(RiftEntities.GOBLIN.get(), GoblinRenderer::new);
        event.registerEntityRenderer(RiftEntities.GOBLIN_SHAMAN.get(), GoblinRenderer::new);
    }
}
