package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.GoblinPatch;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;

public final class PatchedGoblinHeldItemLayer extends PatchedItemInHandLayer<GoblinEntity,GoblinPatch,HumanoidModel<GoblinEntity>> {
    private final ItemStack knife=GoblinHeldItemLayer.knife();
    @Override protected void renderLayer(GoblinPatch patch,GoblinEntity mob,RenderLayer<GoblinEntity,HumanoidModel<GoblinEntity>> layer,
            PoseStack stack,MultiBufferSource buffers,int light,OpenMatrix4f[] poses,float bob,float yRot,float xRot,float partialTicks){
        if(!mob.isBurrowing()){super.renderLayer(patch,mob,layer,stack,buffers,light,poses,bob,yRot,xRot,partialTicks);return;}
        RenderEngine.getInstance().getItemRenderer(knife).renderItemInHand(knife,patch,InteractionHand.MAIN_HAND,poses,buffers,stack,light,partialTicks);
    }
}
