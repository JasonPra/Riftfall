package org.Azkerash.riftfall.facial;

import java.util.function.IntConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Per-mob adapter: world-space eye target -> head-relative angles -> synchronized presentation frame. */
public final class FacialController {
    private final Mob mob;
    private final FaceAnimator animator;
    private final IntConsumer sync;
    private final java.util.function.Supplier<FaceExpression> expressionSource;
    private FaceFrame frame=FaceFrame.NEUTRAL;
    private FaceAnimator.Input lastInput=FaceAnimator.Input.IDLE;
    public FacialController(Mob mob,FacialProfile profile,IntConsumer sync) {
        this(mob,profile,sync,()->null);
    }
    public FacialController(Mob mob,FacialProfile profile,IntConsumer sync,java.util.function.Supplier<FaceExpression> expressionSource) {
        this.expressionSource=java.util.Objects.requireNonNull(expressionSource);
        this.mob=java.util.Objects.requireNonNull(mob);this.sync=java.util.Objects.requireNonNull(sync);
        animator=new FaceAnimator(profile,mob.getUUID().getMostSignificantBits()^mob.getUUID().getLeastSignificantBits());
    }
    public void tick() {
        var target=mob.getTarget();var input=FaceAnimator.Input.IDLE;
        if (!mob.isNoAi() && target!=null && target.isAlive() && target.level()==mob.level() && !target.isSpectator()
                && !(target instanceof Player p && p.isCreative()) && !mob.isAlliedTo(target) && mob.canAttack(target)
                && mob.getSensing().hasLineOfSight(target)) {
            var direction=target.getEyePosition().subtract(mob.getEyePosition());
            double yaw=Math.toDegrees(Math.atan2(direction.z,direction.x))-90;
            double pitch=-Math.toDegrees(Math.atan2(direction.y,Math.sqrt(direction.horizontalDistanceSqr())));
            input=new FaceAnimator.Input(target.getId(),Mth.wrapDegrees((float)(yaw-mob.getYHeadRot())),pitch-mob.getXRot());
        }
        lastInput=input;frame=animator.update(mob.tickCount,input,expressionSource.get());sync.accept(frame.pack());
    }
    public FaceAnimator.Input input() { return lastInput; }
    public FaceAnimator animator() { return animator; }
    public FaceFrame frame() { return frame; }
    public void blinkOnce() { animator.blinkOnce(mob.tickCount); }
}
