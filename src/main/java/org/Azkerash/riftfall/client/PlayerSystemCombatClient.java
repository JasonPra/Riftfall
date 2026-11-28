package org.Azkerash.riftfall.client;

import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import org.Azkerash.riftfall.compat.PlayerSystemCombat;

import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.model.Meshes.MeshAccessor;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Dedicated orc rig/mesh; other humanoids adapt installed geometry and retain original skins. */
@EventBusSubscriber(modid = "riftfall", value = Dist.CLIENT)
public final class PlayerSystemCombatClient {
    private static final Map<String, MeshAccessor<HumanoidMesh>> MESHES = new HashMap<>();

    public static void setup() {
        for (String name : PlayerSystemCombat.HUMANOIDS.keySet()) {
            if (PlayerSystemCombat.type(name) == null) {
                continue;
            }
            MESHES.put(
                    name,
                    MeshAccessor.create(
                            "riftfall",
                            "entity/compat/" + name,
                            ignored -> {
                                var source =
                                        name.equals("orc_warrior")
                                                ? ResourceLocation.fromNamespaceAndPath(
                                                        "riftfall",
                                                        "animmodels/entity/orc_warrior.json")
                                                : ResourceLocation.fromNamespaceAndPath(
                                                        "player_system",
                                                        "geo/mob/" + name + ".geo.json");
                                try (var reader =
                                        Minecraft.getInstance()
                                                .getResourceManager()
                                                .openAsReader(source)) {
                                    var data = JsonParser.parseReader(reader).getAsJsonObject();
                                    var mesh =
                                            name.equals("orc_warrior")
                                                    ? data
                                                    : EncounterHumanoidMesh.convert(data);
                                    return new JsonAssetLoader(mesh, source)
                                            .loadSkinnedMesh(HumanoidMesh::new);
                                } catch (IOException exception) {
                                    throw new IllegalStateException(
                                            "Cannot load encounter geometry " + source, exception);
                                }
                            }));
        }
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY
                .registerEvent(
                        event -> {
                            event.addPatchedEntityRenderer(
                                    org.Azkerash.riftfall.OrcEntities.ORC_WARRIOR.get(),
                                    type ->
                                            new PHumanoidRenderer<
                                                    Mob,
                                                    LivingEntityPatch<Mob>,
                                                    HumanoidModel<Mob>,
                                                    Renderer,
                                                    HumanoidMesh>(
                                                    MESHES.get("orc_warrior"),
                                                    event.getContext(),
                                                    type));
                            MESHES.forEach(
                                    (name, preset) ->
                                            event.addPatchedEntityRenderer(
                                                    PlayerSystemCombat.type(name),
                                                    type ->
                                                            new PHumanoidRenderer<
                                                                    Mob,
                                                                    LivingEntityPatch<Mob>,
                                                                    HumanoidModel<Mob>,
                                                                    Renderer,
                                                                    HumanoidMesh>(
                                                                    MESHES.get(name),
                                                                    event.getContext(),
                                                                    type)));
                        });
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                org.Azkerash.riftfall.OrcEntities.ORC_WARRIOR.get(),
                context -> new Renderer(context, "orc_warrior"));
        PlayerSystemCombat.HUMANOIDS
                .keySet()
                .forEach(
                        name ->
                                event.registerEntityRenderer(
                                        (net.minecraft.world.entity.EntityType)
                                                PlayerSystemCombat.type(name),
                                        context -> new Renderer(context, name)));
    }

    public static final class Renderer extends HumanoidMobRenderer<Mob, HumanoidModel<Mob>> {
        private final ResourceLocation texture;

        Renderer(EntityRendererProvider.Context context, String name) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), .5F);
            texture =
                    ResourceLocation.fromNamespaceAndPath(
                            "player_system", "textures/entity/mob/" + name + ".png");
        }

        @Override
        public ResourceLocation getTextureLocation(Mob entity) {
            return texture;
        }
    }

    private PlayerSystemCombatClient() {}
}
