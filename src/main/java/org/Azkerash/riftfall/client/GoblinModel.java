package org.Azkerash.riftfall.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import org.Azkerash.riftfall.GoblinEntity;

public final class GoblinModel extends HumanoidModel<GoblinEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("riftfall","goblin"),"main");
    public static final ModelLayerLocation SHAMAN_LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("riftfall","goblin_shaman"),"main");
    public GoblinModel(ModelPart root) { super(root); }
    public static LayerDefinition layer() { return layer(false); }
    public static LayerDefinition layer(boolean shaman) {
        MeshDefinition mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);
        PartDefinition root=mesh.getRoot();
        PartDefinition head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4.4F,-8.5F,-4.3F,8.8F,8.5F,8.6F),PartPose.ZERO);
        head.addOrReplaceChild("nose",CubeListBuilder.create().texOffs(0,48).addBox(-1.2F,-4,-6.2F,2.4F,2.5F,2),PartPose.ZERO);
        for (int side : new int[]{-1,1}) {
            var ear=head.addOrReplaceChild(side<0 ? "right_ear" : "left_ear",CubeListBuilder.create().texOffs(48,0)
                .addBox(side<0 ? -3 : 0,-1.5F,-.7F,3,3,1.4F),PartPose.offsetAndRotation(side*4,-5,0,0,0,side*-.35F));
            ear.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(56,0).addBox(side<0 ? -1.5F : 0,-.7F,-.4F,1.5F,1.4F,.8F),PartPose.offset(side*3,-.5F,0));
        }
        if (shaman) {
            head.addOrReplaceChild("bone_helmet",CubeListBuilder.create().texOffs(0,32)
                .addBox(-4.7F,-9.3F,-4.6F,9.4F,2.7F,9.2F),PartPose.ZERO);
            head.addOrReplaceChild("left_tusk",CubeListBuilder.create().texOffs(16,32)
                .addBox(0,-3,-.7F,1.3F,3,1.4F),PartPose.offsetAndRotation(4,-8,-2,0,0,.4F));
            head.addOrReplaceChild("right_tusk",CubeListBuilder.create().texOffs(16,32)
                .addBox(-1.3F,-3,-.7F,1.3F,3,1.4F),PartPose.offsetAndRotation(-4,-8,-2,0,0,-.4F));
            PartDefinition body=root.getChild("body");
            for (int x : new int[]{-2,0,2}) body.addOrReplaceChild("tooth_"+x,CubeListBuilder.create().texOffs(16,32)
                .addBox(-.5F,0,-.5F,1,1.5F,1),PartPose.offsetAndRotation(x,3+Math.abs(x)*-.2F,-2.5F,0,0,x*.15F));
        }
        return LayerDefinition.create(mesh,64,64);
    }
}
