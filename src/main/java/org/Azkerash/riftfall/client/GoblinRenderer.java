package org.Azkerash.riftfall.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.Azkerash.riftfall.GoblinEntity;

public final class GoblinRenderer extends HumanoidMobRenderer<GoblinEntity,HumanoidModel<GoblinEntity>> {
    public GoblinRenderer(EntityRendererProvider.Context context,boolean shaman) { super(context,new GoblinModel(context.bakeLayer(shaman ? GoblinModel.SHAMAN_LAYER : GoblinModel.LAYER)),.22F);addLayer(new GoblinFacialLayer(this)); }
    @Override public ResourceLocation getTextureLocation(GoblinEntity goblin) {
        return GoblinFaceTextures.texture(goblin);
    }
}
