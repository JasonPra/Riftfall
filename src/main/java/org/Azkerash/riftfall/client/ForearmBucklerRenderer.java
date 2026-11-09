package org.Azkerash.riftfall.client;

import com.google.gson.JsonElement;
import net.minecraft.world.InteractionHand;
import org.Azkerash.riftfall.GoblinEntity;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.item.RenderShield;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Strap the buckler to the lower arm instead of applying the sword/palm correction. */
public final class ForearmBucklerRenderer extends RenderShield {
    public ForearmBucklerRenderer(JsonElement skin) { super(skin); }

    @Override public OpenMatrix4f getCorrectionMatrix(LivingEntityPatch<?> patch, InteractionHand hand, OpenMatrix4f[] poses) {
        if (!(patch.getOriginal() instanceof GoblinEntity)) return super.getCorrectionMatrix(patch, hand, poses);
        boolean left = hand == InteractionHand.OFF_HAND;
        var forearm = patch.getArmature().searchJointByName(left ? "Hand_L" : "Hand_R");
        return new OpenMatrix4f(poses[forearm.getId()]).translate(left ? -.13F : .13F, .14F, 0);
    }
}
