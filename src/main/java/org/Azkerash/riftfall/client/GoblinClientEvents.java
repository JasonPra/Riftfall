package org.Azkerash.riftfall.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.Azkerash.riftfall.GoblinEntities;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid = Riftfall.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoblinClientEvents {
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GoblinModel.LAYER,GoblinModel::layer);
        event.registerLayerDefinition(GoblinModel.SHAMAN_LAYER,()->GoblinModel.layer(true));
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent setup) {
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Registry.PATCHED_ITEM.registerEvent(event ->
            event.addItemRenderer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"forearm_buckler"),
                ForearmBucklerRenderer::new));
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(event -> {
            for (var type : java.util.List.of(GoblinEntities.GOBLIN.get(), GoblinEntities.GOBLIN_SHAMAN.get(), GoblinEntities.GOBLIN_ARCHER.get()))
                event.addPatchedEntityRenderer(type, t -> new yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer<>(
                    t==GoblinEntities.GOBLIN_SHAMAN.get() ? GoblinMeshes.SHAMAN : GoblinMeshes.BODY,event.getContext(),t));
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GoblinEntities.GOBLIN_ARCHER.get(), context->new GoblinRenderer(context,false));
        event.registerEntityRenderer(GoblinEntities.GOBLIN.get(), context->new GoblinRenderer(context,false));
        event.registerEntityRenderer(GoblinEntities.GOBLIN_SHAMAN.get(), context->new GoblinRenderer(context,true));
    }
}
