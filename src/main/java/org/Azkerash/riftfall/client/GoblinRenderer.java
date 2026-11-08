package org.Azkerash.riftfall.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.Azkerash.riftfall.GoblinEntity;

public final class GoblinRenderer extends HumanoidMobRenderer<GoblinEntity,HumanoidModel<GoblinEntity>> {
    public GoblinRenderer(EntityRendererProvider.Context context) { super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),.35F); }
    @Override public ResourceLocation getTextureLocation(GoblinEntity goblin) {
        return ResourceLocation.fromNamespaceAndPath("riftfall","textures/entity/"+(goblin.isShaman()?"goblin_shaman":"goblin")+".png");
    }
}
