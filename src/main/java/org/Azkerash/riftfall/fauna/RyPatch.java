package org.Azkerash.riftfall.fauna;

import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;

/** Epic Fight owns synchronized actions/staggers; the utility goal owns combat decisions. */
public final class RyPatch extends MobPatch<RyEntity> {
    public RyPatch(RyEntity ry) { super(ry,MagicBeastFaction.MAGIC_BEASTS); }
    @Override protected void initAI() {}
    @Override public void initAnimator(Animator a) {
        a.addLivingAnimation(LivingMotions.IDLE,RyAnimations.IDLE);
        a.addLivingAnimation(LivingMotions.WALK,RyAnimations.WALK);
        a.addLivingAnimation(LivingMotions.RUN,RyAnimations.RUN);
        a.addLivingAnimation(LivingMotions.SIT,RyAnimations.SIT);
        a.addLivingAnimation(LivingMotions.DEATH,RyAnimations.DEATH);
    }
    @Override public void updateMotion(boolean inaction) {
        commonMobUpdateMotion(inaction);
        if(original.isInSittingPose()) currentLivingMotion=LivingMotions.SIT;
        else if(original.isSprinting()&&!isStunned()&&!getEntityState().inaction()) currentLivingMotion=LivingMotions.RUN;
    }
    @Override public AssetAccessor<? extends StaticAnimation> getHitAnimation(StunType type) { return RyAnimations.HURT; }
    @Override public yesman.epicfight.api.utils.math.OpenMatrix4f getModelMatrix(float partial) {
        float s=original.getScale();return super.getModelMatrix(partial).scale(s,s,s);
    }
    public static RyPatch of(RyEntity ry) { return yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(ry,RyPatch.class); }
    public static boolean interrupted(RyEntity ry) { var p=of(ry);return ry.hurtTime>0 || p!=null&&p.isStunned(); }
    public static void play(RyEntity ry,AssetAccessor<? extends StaticAnimation> animation) {
        var p=of(ry);if(p!=null&&animation!=null)p.playAnimationSynchronized(animation,0);
    }
}
