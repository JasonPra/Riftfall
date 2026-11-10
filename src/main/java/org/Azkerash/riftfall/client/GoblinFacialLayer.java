package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.facial.FaceFrame;

public final class GoblinFacialLayer extends RenderLayer<GoblinEntity,HumanoidModel<GoblinEntity>> {
    private final ArticulatedFaceRig rig=new ArticulatedFaceRig(GoblinFaceBinding.LAYOUT,GoblinFaceBinding.MOUTH,GoblinFaceBinding.STYLE);
    private static final class History {int tick;FaceFrame previous,current;History(int tick,FaceFrame frame){this.tick=tick;previous=current=frame;}}
    private final java.util.Map<GoblinEntity,History> history=new java.util.WeakHashMap<>();
    public GoblinFacialLayer(RenderLayerParent<GoblinEntity,HumanoidModel<GoblinEntity>> parent){super(parent);}
    public void renderFace(GoblinEntity mob,PoseStack stack,MultiBufferSource buffers,int light,float partial) {
        if(mob.isInvisible())return;
        var state=history.computeIfAbsent(mob,m->new History(m.tickCount,m.facialFrame()));
        if(state.tick!=mob.tickCount){state.previous=state.current;state.current=mob.facialFrame();state.tick=mob.tickCount;}
        rig.render(state.previous,state.current,partial,stack,buffers,light,GoblinFaceTextures.material(),GoblinFaceTextures.base(mob.isShaman()?"goblin_shaman":mob.isArcher()?"goblin_archer":"goblin"));
    }
    @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,GoblinEntity mob,float limb,float amount,float partial,float age,float yaw,float pitch) {
        stack.pushPose();getParentModel().head.translateAndRotate(stack);renderFace(mob,stack,buffers,light,partial);stack.popPose();
    }
}
