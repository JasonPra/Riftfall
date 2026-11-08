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
    @Override public void updateMotion(boolean considerInaction) { if (original.isArcher()) commonAggressiveRangedMobUpdateMotion(considerInaction);
        else commonAggressiveMobUpdateMotion(considerInaction); }
}
