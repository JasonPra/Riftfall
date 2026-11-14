package org.Azkerash.riftfall.client.fauna;

import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Direwolf proportions, retaining the native wolf rig for walking, sitting and pups. */
public final class RyModel extends WolfModel<net.minecraft.world.entity.animal.Wolf> {
    private float renderPartial;
    private final net.minecraft.client.model.geom.ModelPart root;
    private final java.util.Map<String,net.minecraft.client.model.geom.ModelPart> joints;
    public RyModel(net.minecraft.client.model.geom.ModelPart root) {
        super(root);this.root=root;
        joints=java.util.Map.of("Jaw",root.getChild("head").getChild("real_head").getChild("jaw"),"Head",root.getChild("head"),"Chest",root.getChild("upper_body"),
            "Body",root.getChild("body"),"FrontR",root.getChild("right_front_leg"),"FrontL",root.getChild("left_front_leg"),
            "BackR",root.getChild("right_hind_leg"),"BackL",root.getChild("left_hind_leg"),"Tail",root.getChild("tail"));
    }
    @Override public void prepareMobModel(net.minecraft.world.entity.animal.Wolf wolf,float swing,float amount,float partial) {
        renderPartial=partial;
        root.getAllParts().forEach(net.minecraft.client.model.geom.ModelPart::resetPose);
        super.prepareMobModel(wolf,swing,amount,partial);
    }
    @Override public void setupAnim(net.minecraft.world.entity.animal.Wolf wolf,float limbSwing,float amount,float age,float yaw,float pitch) {
        super.setupAnim(wolf,limbSwing,amount,age,yaw,pitch);
        if(!(wolf instanceof org.Azkerash.riftfall.fauna.RyEntity ry))return;
        var patch=org.Azkerash.riftfall.fauna.RyPatch.of(ry);if(patch==null)return;
        // Render the very same synchronized Epic Fight pose on our detailed quadruped model.
        // Native gait, pup proportions, collar and armour remain on the normal wolf renderer path.
        var pose=patch.getAnimator().getPose(renderPartial);
        joints.forEach((name,part)->{
            if(!pose.hasTransform(name))return;
            var rotation=pose.get(name).rotation().getEulerAnglesXYZ(new org.joml.Vector3f());
            // Epic Fight joint quaternions and ModelPart Euler rotations use opposing rotation conventions.
            part.xRot-=rotation.x;part.yRot-=rotation.y;part.zRot-=rotation.z;
        });
    }
    public static LayerDefinition layer() {
        MeshDefinition mesh = WolfModel.createMeshDefinition(CubeDeformation.NONE);
        var root = mesh.getRoot();
        // A broad skull, long muzzle and small upright ears; cheek tufts frame the face.
        root.getChild("head").addOrReplaceChild("real_head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-2, -3, -2, 6, 6, 4, new CubeDeformation(.35F))
            .texOffs(0, 10).addBox(-.5F, -.25F, -5.5F, 3, 2, 4, new CubeDeformation(.15F))
            .texOffs(16, 14).addBox(-2.25F, -5.5F, 0, 2, 2, 1, new CubeDeformation(.15F))
            .texOffs(16, 14).addBox(2.25F, -5.5F, 0, 2, 2, 1, new CubeDeformation(.15F))
            .texOffs(52, 0).addBox(-3, -.5F, 0, 2, 3, 3)
            .texOffs(52, 0).mirror().addBox(3, -.5F, 0, 2, 3, 3), PartPose.ZERO);
        root.getChild("head").getChild("real_head").addOrReplaceChild("jaw", CubeListBuilder.create()
            .texOffs(0,11).addBox(-1.5F,0,-4,3,1,4),PartPose.offset(1,1.75F,-1.5F));
        // Deep chest and layered ruff; narrower loin avoids a uniformly inflated body.
        root.addOrReplaceChild("upper_body", CubeListBuilder.create()
            .texOffs(21, 0).addBox(-3, -3.5F, -3, 8, 6, 7)
            .texOffs(52, 0).addBox(-3.5F, -4.5F, -2, 3, 4, 3)
            .texOffs(52, 0).addBox(2.5F, -4.5F, -2, 3, 4, 3)
            .texOffs(52, 0).addBox(-2.5F, -6, -1, 2, 4, 2)
            .texOffs(52, 0).addBox(2.5F, -6, -1, 2, 4, 2)
            .texOffs(52, 0).addBox(-1, -6.5F, 0, 4, 3, 2),
            PartPose.offsetAndRotation(-1, 14, -3, (float)Math.PI / 2, 0, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(18, 14).addBox(-2.75F, -3, -3, 5.5F, 11, 6),
            PartPose.offsetAndRotation(0, 14, 2, (float)Math.PI / 2, 0, 0));
        String[] legs = {"right_hind_leg", "left_hind_leg", "right_front_leg", "left_front_leg"};
        for (int i = 0; i < legs.length; i++) {
            root.addOrReplaceChild(legs[i], CubeListBuilder.create()
                .texOffs(0, 18).addBox(-0.25F, 0, -1.25F, 2.5F, 7, 2.5F)
                .texOffs(0, 24).addBox(-0.5F, 6, -2, 3, 2, 4),
                PartPose.offset(i % 2 == 0 ? -2.5F : 0.5F, 16, i < 2 ? 7 : -4));
        }
        root.getChild("tail").addOrReplaceChild("real_tail", CubeListBuilder.create()
            .texOffs(9, 18).addBox(-0.5F, 0, -1.5F, 3, 6, 3)
            .texOffs(48, 18).addBox(-.75F, 4, -1.75F, 3.5F, 6, 3.5F)
            .texOffs(48, 18).addBox(-.25F, 9, -1.25F, 2.5F, 4, 2.5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

}
