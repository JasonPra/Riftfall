package org.Azkerash.riftfall;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

/** Keep goblin melee/spell AI; Epic Fight supplies the synchronized skeleton and reactions. */
public final class GoblinPatch extends HumanoidMobPatch<GoblinEntity> {
    public GoblinPatch(GoblinEntity goblin) { super(goblin,Factions.NEUTRAL); }
    @Override protected void initAI() {}
    @Override public void setAIAsInfantry(boolean armed) {}
    @Override public void initAnimator(Animator animator) {
        // Base initialization binds held items to Tool_R/Tool_L. Without it they
        // attach to the root joint, behind the body, and ignore the casting arms.
        super.initAnimator(animator);
        commonAggresiveMobAnimatorInit(animator);
        if (original.isArcher()) {
            animator.addLivingAnimation(yesman.epicfight.api.animation.LivingMotions.AIM,yesman.epicfight.gameasset.Animations.BIPED_BOW_AIM);
            animator.addLivingAnimation(yesman.epicfight.api.animation.LivingMotions.SHOT,yesman.epicfight.gameasset.Animations.BIPED_BOW_SHOT);
        }
    }
    @Override public yesman.epicfight.api.utils.math.OpenMatrix4f getModelMatrix(float partialTick) {
        float scale=original.getScale();
        return super.getModelMatrix(partialTick).scale(scale,scale,scale);
    }
    @Override public void poseTick(yesman.epicfight.api.animation.types.DynamicAnimation animation,
            yesman.epicfight.api.animation.Pose pose,float elapsedTime,float partialTick) {
        super.poseTick(animation,pose,elapsedTime,partialTick);
        if (!original.isShaman() || animation.isMainFrameAnimation() || animation.isLinkAnimation() || isStunned()) return;
        // A relaxed straight arm puts an upright staff through the forearm. Bend
        // the elbow forward, then counter-rotate the wrist to keep the shaft upright.
        // Action animations own the entire arm during casting, club swings and hits.
        if (original.getMainHandItem().is(GoblinItems.BONE_STAFF.get())) carryStaff(pose,"Hand_R","Tool_R");
        if (original.getOffhandItem().is(GoblinItems.BONE_STAFF.get())) carryStaff(pose,"Hand_L","Tool_L");
    }
    private static void carryStaff(yesman.epicfight.api.animation.Pose pose,String forearm,String tool) {
        var elbow=pose.orElseEmpty(forearm);
        var wrist=pose.orElseEmpty(tool);
        elbow.rotation().rotateX((float)Math.toRadians(55));
        wrist.rotation().rotateX((float)Math.toRadians(-55));
        pose.putJointData(forearm,elbow); pose.putJointData(tool,wrist);
    }
    @Override public void updateMotion(boolean considerInaction) { if (original.isArcher()) commonAggressiveRangedMobUpdateMotion(considerInaction);
        else commonAggressiveMobUpdateMotion(considerInaction); }
}
