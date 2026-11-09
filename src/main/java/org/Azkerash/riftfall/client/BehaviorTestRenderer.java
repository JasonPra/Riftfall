package org.Azkerash.riftfall.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import org.Azkerash.riftfall.BehaviorTestEntity;

/** Vanilla humanoid placeholder; debug nameplate communicates the active behavior. */
public final class BehaviorTestRenderer extends HumanoidMobRenderer<BehaviorTestEntity,HumanoidModel<BehaviorTestEntity>> {
    public BehaviorTestRenderer(EntityRendererProvider.Context context) {
        super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),.4F);
    }
    @Override public ResourceLocation getTextureLocation(BehaviorTestEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    }
}
