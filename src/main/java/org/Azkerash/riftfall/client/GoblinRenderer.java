package org.Azkerash.riftfall.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.Azkerash.riftfall.GoblinEntity;

public final class GoblinRenderer extends HumanoidMobRenderer<GoblinEntity,HumanoidModel<GoblinEntity>> {
    public GoblinRenderer(EntityRendererProvider.Context context,boolean shaman) { super(context,new GoblinModel(context.bakeLayer(shaman ? GoblinModel.SHAMAN_LAYER : GoblinModel.LAYER)),.22F);layers.removeIf(layer->layer instanceof net.minecraft.client.renderer.entity.layers.ItemInHandLayer);addLayer(new GoblinHeldItemLayer(this,context.getItemInHandRenderer()));addLayer(new GoblinFacialLayer(this));addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
            new HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_OUTER_ARMOR)),context.getModelManager())); }
    @Override protected void scale(GoblinEntity mob,com.mojang.blaze3d.vertex.PoseStack stack,float partialTick) {
        stack.translate(0,-mob.burrowOffset(partialTick),0);
    }
    @Override public ResourceLocation getTextureLocation(GoblinEntity goblin) {
        return GoblinFaceTextures.texture(goblin);
    }
}
