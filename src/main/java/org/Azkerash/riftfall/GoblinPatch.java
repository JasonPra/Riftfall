package org.Azkerash.riftfall;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

/** Keep goblin melee/spell AI; Epic Fight supplies the synchronized skeleton and reactions. */
public final class GoblinPatch extends HumanoidMobPatch<GoblinEntity> {
    public GoblinPatch(GoblinEntity goblin) { super(goblin,Factions.NEUTRAL); }
    @Override protected void initAI() {}
    @Override public void setAIAsInfantry(boolean armed) {}
    @Override public yesman.epicfight.api.utils.AttackResult tryHurt(net.minecraft.world.damagesource.DamageSource source,float amount) {
        if(original.isFullyBuried() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))
            return yesman.epicfight.api.utils.AttackResult.missed(amount);
        return GoblinShieldGuard.defend(this,source,super.tryHurt(source,amount));
    }
    @Override public void initAnimator(Animator animator) {
        // Base initialization binds held items to Tool_R/Tool_L. Without it they
        // attach to the root joint, behind the body, and ignore the casting arms.
        super.initAnimator(animator);
        commonAggresiveMobAnimatorInit(animator);
        animator.addLivingAnimation(yesman.epicfight.api.animation.LivingMotions.RUN,GoblinAnimations.PANIC_RUN);
        animator.addLivingAnimation(yesman.epicfight.api.animation.LivingMotions.BLOCK,yesman.epicfight.gameasset.Animations.BIPED_BLOCK);
        if (original.isArcher()) {
            animator.addLivingAnimation(yesman.epicfight.api.animation.LivingMotions.AIM,yesman.epicfight.gameasset.Animations.BIPED_BOW_AIM);
            animator.addLivingAnimation(yesman.epicfight.api.animation.LivingMotions.SHOT,yesman.epicfight.gameasset.Animations.BIPED_BOW_SHOT);
        }
    }
    @Override public yesman.epicfight.api.utils.math.OpenMatrix4f getModelMatrix(float partialTick) {
        float scale=original.getScale();
        return super.getModelMatrix(partialTick).scale(scale,scale,scale).translate(0,-original.burrowOffset(partialTick),0);
    }
    @Override public void poseTick(yesman.epicfight.api.animation.types.DynamicAnimation animation,
            yesman.epicfight.api.animation.Pose pose,float elapsedTime,float partialTick) {
        super.poseTick(animation,pose,elapsedTime,partialTick);
        if (!original.isAlive() || original.isNoAi() || isStunned()) return;
        boolean action=animation instanceof yesman.epicfight.api.animation.types.ActionAnimation;
        if (original.isAggressive() || action)
            GoblinCombatTremor.apply(pose,(original.tickCount+partialTick)/20F,original.getId(),action,original.tremorConfidence(partialTick));
    }
    @Override public void postTickClient() {
        super.postTickClient();
        org.Azkerash.riftfall.client.GoblinBurrowPlayback.update(this);
    }
    @Override public void updateMotion(boolean considerInaction) {
        if(original.isBurrowing() && !isStunned()) {
            currentLivingMotion=yesman.epicfight.api.animation.LivingMotions.INACTION;
            currentCompositeMotion=yesman.epicfight.api.animation.LivingMotions.NONE;
            return;
        }
        if (original.isArcher()) commonAggressiveRangedMobUpdateMotion(considerInaction);
        else commonAggressiveMobUpdateMotion(considerInaction);
        if((original.isPanicFleeing() || original.isArcher() && original.isSprinting())
                && !isStunned() && original.getDeltaMovement().horizontalDistanceSqr()>.0001) {
            currentLivingMotion=yesman.epicfight.api.animation.LivingMotions.RUN;
            currentCompositeMotion=yesman.epicfight.api.animation.LivingMotions.NONE;return;
        }
        if (original.isAlive() && original.isBlocking() && !getEntityState().inaction())
            currentCompositeMotion = yesman.epicfight.api.animation.LivingMotions.BLOCK;
    }
}
