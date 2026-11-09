package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.Azkerash.riftfall.facial.*;

/** Reusable named facial pivots. Flat eye whites stay fixed; pupil geometry slides across them. */
public final class ArticulatedFaceRig {
    public record Layout(float eyeSpacing,float eyeY,float faceZ,float mouthY) {}
    private final net.minecraft.client.model.geom.ModelPart root;
    private final ModelPart[] pupils=new ModelPart[2],lids=new ModelPart[2],brows=new ModelPart[2];
    public record MouthLayout(int textureWidth,int textureHeight,int u,int v,float width,float height,float scaleX,float scaleY) {}
    private final ModelPart jaw;
    private final MouthLayout mouth;
    public ArticulatedFaceRig(Layout layout,MouthLayout mouth) {
        this.mouth=mouth;
        MeshDefinition mesh=new MeshDefinition();var base=mesh.getRoot();
        for(int i=0;i<2;i++) {
            String side=i==0?"left":"right";
            var socket=base.addOrReplaceChild(side+"_socket",CubeListBuilder.create().texOffs(0,0).addBox(-1.1F,-.9F,-.15F,2.2F,1.8F,.15F),PartPose.offset((i==0?-1:1)*layout.eyeSpacing(),layout.eyeY(),layout.faceZ()));
            socket.addOrReplaceChild("pupil",CubeListBuilder.create().texOffs(8,0).addBox(-.3F,-.5F,-.23F,.6F,1F,.1F),PartPose.ZERO);
            socket.addOrReplaceChild("upper_lid",CubeListBuilder.create().texOffs(16,0).addBox(-1.15F,-1.9F,-.3F,2.3F,1.9F,.15F),PartPose.offset(0,-.9F,0));
            socket.addOrReplaceChild("brow",CubeListBuilder.create().texOffs(24,0).addBox(-1.2F,-.25F,-.4F,2.4F,.5F,.2F),PartPose.offset(0,-1.25F,0));
        }
        root=LayerDefinition.create(mesh,48,8).bakeRoot();
        // The mouth keeps its original atlas pixels and flat silhouette, on a small hinged panel.
        MeshDefinition mouthMesh=new MeshDefinition();
        mouthMesh.getRoot().addOrReplaceChild("jaw",CubeListBuilder.create().texOffs(mouth.u(),mouth.v())
            .addBox(-mouth.width()/2,0,0,mouth.width(),mouth.height(),.02F),
            PartPose.offset(0,layout.mouthY(),layout.faceZ()+.13F));
        jaw=LayerDefinition.create(mouthMesh,mouth.textureWidth(),mouth.textureHeight()).bakeRoot().getChild("jaw");
        for(int i=0;i<2;i++) {var socket=root.getChild(i==0?"left_socket":"right_socket");pupils[i]=socket.getChild("pupil");lids[i]=socket.getChild("upper_lid");brows[i]=socket.getChild("brow");}
    }
    public void render(FaceFrame previous,FaceFrame current,float alpha,PoseStack stack,MultiBufferSource buffers,int light,net.minecraft.resources.ResourceLocation material,net.minecraft.resources.ResourceLocation mouthMaterial) {
        var a=FacialRigPose.from(previous);var b=FacialRigPose.from(current);
        for(int i=0;i<2;i++) {
            pupils[i].x=mix(a.pupilX(),b.pupilX(),alpha);pupils[i].y=mix(a.pupilY(),b.pupilY(),alpha);
            lids[i].y=-.9F+mix(a.lidClosure(),b.lidClosure(),alpha)*1.8F;
            brows[i].y=-1.25F+mix(a.browHeight(),b.browHeight(),alpha);
            brows[i].zRot=(i==0?1:-1)*mix(a.browAngle(),b.browAngle(),alpha);
        }
        float opening=mix(a.jawAngle(),b.jawAngle(),alpha);
        jaw.xRot=-opening;jaw.y=jaw.getInitialPose().y+opening*1.1F;
        jaw.xScale=mouth.scaleX();jaw.yScale=mouth.scaleY();
        root.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(material)),light,OverlayTexture.NO_OVERLAY);
        jaw.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(mouthMaterial)),light,OverlayTexture.NO_OVERLAY);
    }
    private static float mix(float a,float b,float t) {return a+(b-a)*t;}
}
