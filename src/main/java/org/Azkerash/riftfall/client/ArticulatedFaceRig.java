package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.Azkerash.riftfall.facial.*;

/** Reusable named facial pivots. Flat eye whites stay fixed; pupil geometry slides across them. */
public final class ArticulatedFaceRig {
    public record Layout(float eyeSpacing,float eyeY,float faceZ) {}
    private final net.minecraft.client.model.geom.ModelPart root;
    private final ModelPart[] pupils=new ModelPart[2],lids=new ModelPart[2],brows=new ModelPart[2];
    public record Style(float eyeHeight,float pupilWidth,float pupilHeight,float pupilTravelX,float pupilTravelY,
            float restingBrowAngle,float restingLidClosure,float browWidth,float browThickness) {}
    private final ModelPart jaw;
    private final AtlasPanel mouth;
    private final Style style;
    public ArticulatedFaceRig(Layout layout,AtlasPanel mouth,Style style) {
        this.mouth=mouth;this.style=style;
        MeshDefinition mesh=new MeshDefinition();var base=mesh.getRoot();
        for(int i=0;i<2;i++) {
            String side=i==0?"left":"right";
            var socket=base.addOrReplaceChild(side+"_socket",CubeListBuilder.create().texOffs(24,0)
                .addBox(-1.2F,-style.eyeHeight()/2-.1F,-.1F,2.4F,style.eyeHeight()+.2F,.1F)
                .texOffs(0,0).addBox(-1.1F,-style.eyeHeight()/2,-.2F,2.2F,style.eyeHeight(),.1F),
                PartPose.offset((i==0?-1:1)*layout.eyeSpacing(),layout.eyeY(),layout.faceZ()));
            socket.addOrReplaceChild("pupil",CubeListBuilder.create().texOffs(8,0)
                .addBox(-style.pupilWidth()/2,-style.pupilHeight()/2,-.3F,style.pupilWidth(),style.pupilHeight(),.05F),PartPose.ZERO);
            var lid=socket.addOrReplaceChild("upper_lid",CubeListBuilder.create().texOffs(16,0)
                .addBox(-1.2F,-style.eyeHeight()-.4F,-.4F,2.4F,style.eyeHeight()+.4F,.1F),PartPose.offset(0,-style.eyeHeight()/2,0));
            lid.addOrReplaceChild("edge",CubeListBuilder.create().texOffs(24,0).addBox(-1.2F,-.15F,-.42F,2.4F,.15F,.1F),PartPose.ZERO);
            socket.addOrReplaceChild("brow",CubeListBuilder.create().texOffs(24,0)
                .addBox(-style.browWidth()/2,-style.browThickness()/2,-.5F,style.browWidth(),style.browThickness(),.1F),
                PartPose.offset(0,-style.eyeHeight()/2-.5F,0));
        }
        root=LayerDefinition.create(mesh,48,8).bakeRoot();
        MeshDefinition mouthMesh=new MeshDefinition();
        mouthMesh.getRoot().addOrReplaceChild("jaw",CubeListBuilder.create(),PartPose.offset(0,mouth.top(),mouth.depth()));
        jaw=LayerDefinition.create(mouthMesh,16,16).bakeRoot().getChild("jaw");
        for(int i=0;i<2;i++) {var socket=root.getChild(i==0?"left_socket":"right_socket");pupils[i]=socket.getChild("pupil");lids[i]=socket.getChild("upper_lid");brows[i]=socket.getChild("brow");}
    }
    public void render(FaceFrame previous,FaceFrame current,float alpha,PoseStack stack,MultiBufferSource buffers,int light,net.minecraft.resources.ResourceLocation material,net.minecraft.resources.ResourceLocation mouthMaterial) {
        var a=FacialRigPose.from(previous);var b=FacialRigPose.from(current);
        for(int i=0;i<2;i++) {
            pupils[i].x=mix(a.pupilX(),b.pupilX(),alpha)/.64F*style.pupilTravelX();
            pupils[i].y=mix(a.pupilY(),b.pupilY(),alpha)/.3F*style.pupilTravelY();
            float closure=mix(closure(previous,a),closure(current,b),alpha);
            float browAngle=mix(browAngle(previous,a),browAngle(current,b),alpha);
            lids[i].y=-style.eyeHeight()/2+closure*style.eyeHeight();
            lids[i].zRot=(i==0?1:-1)*browAngle*.45F*(1-closure);
            brows[i].y=-style.eyeHeight()/2-.5F+mix(a.browHeight(),b.browHeight(),alpha);
            brows[i].zRot=(i==0?1:-1)*browAngle;
        }
        float opening=mix(a.jawAngle(),b.jawAngle(),alpha);
        jaw.xRot=-opening;jaw.y=jaw.getInitialPose().y+opening*1.1F;
        root.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(material)),light,OverlayTexture.NO_OVERLAY);
        stack.pushPose();jaw.translateAndRotate(stack);
        var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(mouthMaterial));
        var pose=stack.last();float left=mouth.left()/16,right=(mouth.left()+mouth.modelWidth())/16,bottom=mouth.modelHeight()/16;
        vertices.addVertex(pose,left,0,0).setColor(-1).setUv(mouth.uLeft(),mouth.v0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,-1);
        vertices.addVertex(pose,left,bottom,0).setColor(-1).setUv(mouth.uLeft(),mouth.v1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,-1);
        vertices.addVertex(pose,right,bottom,0).setColor(-1).setUv(mouth.uRight(),mouth.v1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,-1);
        vertices.addVertex(pose,right,0,0).setColor(-1).setUv(mouth.uRight(),mouth.v0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,-1);
        stack.popPose();
    }
    private boolean stern(FaceFrame frame){return frame.expression()==FaceExpression.NEUTRAL || frame.expression()==FaceExpression.ANGRY;}
    private float closure(FaceFrame frame,FacialRigPose pose){return Math.max(pose.lidClosure(),stern(frame)?style.restingLidClosure():0);}
    private float browAngle(FaceFrame frame,FacialRigPose pose){return pose.browAngle()+(stern(frame)?style.restingBrowAngle():0);}
    private static float mix(float a,float b,float t) {return FacialRigPose.interpolate(a,b,t);}
}
