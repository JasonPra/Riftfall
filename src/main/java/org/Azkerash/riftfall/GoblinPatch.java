package org.Azkerash.riftfall;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

/** Keep goblin melee/spell AI; Epic Fight supplies the synchronized skeleton and reactions. */
public final class GoblinPatch extends HumanoidMobPatch<GoblinEntity> {
    public GoblinPatch(GoblinEntity goblin) { super(goblin,Factions.NEUTRAL); }
    @Override protected void initAI() {}
    @Override public void setAIAsInfantry(boolean armed) {}
    @Override public void initAnimator(Animator animator) { commonAggresiveMobAnimatorInit(animator); }
    @Override public void updateMotion(boolean considerInaction) { commonAggressiveMobUpdateMotion(considerInaction); }
}
