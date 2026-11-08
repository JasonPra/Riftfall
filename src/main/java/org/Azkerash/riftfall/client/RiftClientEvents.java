package org.Azkerash.riftfall.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.Azkerash.riftfall.RiftEntities;
import org.Azkerash.riftfall.Riftfall;

@EventBusSubscriber(modid = Riftfall.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RiftClientEvents {
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GoblinModel.LAYER,GoblinModel::layer);
        event.registerLayerDefinition(GoblinModel.SHAMAN_LAYER,()->GoblinModel.layer(true));
    }
    @SubscribeEvent public static void geometry(net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders event) {
        event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"soul_crystal"),
            (net.neoforged.neoforge.client.model.geometry.IGeometryLoader<SoulCrystalGeometry>)(json, context) -> new SoulCrystalGeometry(
                net.minecraft.util.GsonHelper.getAsInt(json,"crystals",5),net.minecraft.util.GsonHelper.getAsInt(json,"variant",0),
                net.minecraft.util.GsonHelper.getAsInt(json,"connections",0),net.minecraft.util.GsonHelper.getAsString(json,"part","full")));
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent setup) {
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(event -> {
            for (var type : java.util.List.of(RiftEntities.GOBLIN.get(), RiftEntities.GOBLIN_SHAMAN.get(), RiftEntities.GOBLIN_ARCHER.get()))
                event.addPatchedEntityRenderer(type, t -> new yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer<>(
                    t==RiftEntities.GOBLIN_SHAMAN.get() ? GoblinMeshes.SHAMAN : GoblinMeshes.BODY,event.getContext(),t));
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RiftEntities.RIFT.get(), RiftRenderer::new);
        event.registerEntityRenderer(RiftEntities.GOBLIN_ARCHER.get(), context->new GoblinRenderer(context,false));
        event.registerEntityRenderer(RiftEntities.GOBLIN.get(), context->new GoblinRenderer(context,false));
        event.registerEntityRenderer(RiftEntities.GOBLIN_SHAMAN.get(), context->new GoblinRenderer(context,true));
    }
}
