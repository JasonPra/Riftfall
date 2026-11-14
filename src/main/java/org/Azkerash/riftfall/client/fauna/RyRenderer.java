package org.Azkerash.riftfall.client.fauna;

import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.Azkerash.riftfall.fauna.ManaheartFauna;

@EventBusSubscriber(modid="riftfall", value=Dist.CLIENT)
public final class RyRenderer extends WolfRenderer {
    private static final ResourceLocation FUR = ResourceLocation.fromNamespaceAndPath("riftfall", "textures/entity/ry.png");
    private static final RenderType EYES = RenderType.eyes(ResourceLocation.fromNamespaceAndPath("riftfall", "textures/entity/ry_glow.png"));

    public RyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RyModel(RyModel.layer().bakeRoot());
        this.shadowRadius = .65F;
        this.addLayer(new EyesLayer<Wolf, WolfModel<Wolf>>(this) {
            @Override public RenderType renderType() { return EYES; }
        });
    }

    @Override public ResourceLocation getTextureLocation(Wolf wolf) { return FUR; }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ManaheartFauna.RY.get(), RyRenderer::new);
    }
}
