package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.GoblinPatch;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.api.utils.math.*;

/** Bind the independent face pivots to Epic Fight's animated Head joint. */
public final class PatchedGoblinFacialLayer extends PatchedLayer<GoblinEntity,GoblinPatch,HumanoidModel<GoblinEntity>,GoblinFacialLayer> {
    @Override protected void renderLayer(GoblinPatch patch,GoblinEntity mob,GoblinFacialLayer layer,PoseStack stack,MultiBufferSource buffers,int light,OpenMatrix4f[] poses,float bob,float yRot,float xRot,float partialTicks) {
        var head=patch.getArmature().searchJointByName("Head");
        if(head==null)return;
        var transform=new OpenMatrix4f().scale(new Vec3f(-1,-1,1)).mulFront(poses[head.getId()]);
        stack.pushPose();MathUtils.mulStack(stack,transform);
        stack.translate(0,GoblinFaceBinding.EPIC_Y_OFFSET/16,GoblinFaceBinding.EPIC_Z_OFFSET/16);
        stack.scale(1,GoblinFaceBinding.EPIC_Y_SCALE,1);layer.renderFace(mob,stack,buffers,light,partialTicks);stack.popPose();
    }
}
