package org.Azkerash.riftfall.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.Azkerash.riftfall.GoblinEntities;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid = Riftfall.MODID, value = Dist.CLIENT)
public final class GoblinClientEvents {
    @SubscribeEvent public static void reload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{GoblinFaceTextures.clear();org.Azkerash.riftfall.shield.ShieldModels.clear();});
    }
    @SubscribeEvent public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(HeldItemDebug.KEY);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GoblinModel.LAYER,GoblinModel::layer);
        event.registerLayerDefinition(GoblinModel.SHAMAN_LAYER,()->GoblinModel.layer(true));
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent setup) {
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Camera.BUILD_TRANSFORM_POST.registerEvent(ThirdPersonCamera::build);
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Registry.PATCHED_ITEM.registerEvent(event ->
            event.addItemRenderer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"forearm_buckler"),
                ForearmBucklerRenderer::new));
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(event -> {
            for (var type : java.util.List.of(GoblinEntities.GOBLIN.get(), GoblinEntities.GOBLIN_SHAMAN.get(), GoblinEntities.GOBLIN_ARCHER.get()))
                event.addPatchedEntityRenderer(type, t -> { var renderer=new yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer<org.Azkerash.riftfall.GoblinEntity,org.Azkerash.riftfall.GoblinPatch,net.minecraft.client.model.HumanoidModel<org.Azkerash.riftfall.GoblinEntity>,GoblinRenderer,yesman.epicfight.client.mesh.HumanoidMesh>(
                    t==GoblinEntities.GOBLIN_SHAMAN.get() ? GoblinMeshes.SHAMAN : GoblinMeshes.BODY,event.getContext(),t);
                    renderer.addPatchedLayer(GoblinFacialLayer.class,new PatchedGoblinFacialLayer());
                    renderer.addPatchedLayer(GoblinHeldItemLayer.class,new PatchedGoblinHeldItemLayer());return renderer;});
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(org.Azkerash.riftfall.BehaviorTestEntities.TESTER.get(),BehaviorTestRenderer::new);
        event.registerEntityRenderer(GoblinEntities.GOBLIN_ARCHER.get(), context->new GoblinRenderer(context,false));
        event.registerEntityRenderer(GoblinEntities.GOBLIN.get(), context->new GoblinRenderer(context,false));
        event.registerEntityRenderer(GoblinEntities.GOBLIN_SHAMAN.get(), context->new GoblinRenderer(context,true));
    }
}
