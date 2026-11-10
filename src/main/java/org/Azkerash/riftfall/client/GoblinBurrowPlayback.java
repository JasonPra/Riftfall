package org.Azkerash.riftfall.client;

import org.Azkerash.riftfall.GoblinAnimations;
import org.Azkerash.riftfall.GoblinPatch;

/** Recover the full-body action from entity state, including observers that missed its start packet. */
public final class GoblinBurrowPlayback {
    public static void update(GoblinPatch patch) {
        var mob=patch.getOriginal();
        if(!mob.isAlive() || patch.isStunned() || GoblinAnimations.BURROW==null)return;
        var animator=patch.getClientAnimator();
        boolean playing=animator.isPlaying(GoblinAnimations.BURROW);
        if(mob.isBurrowing()) {
            if(!playing) {
                // Resume at the synchronized progress; never restart a late observer at the standing pose.
                animator.offAllLayers();
                animator.playAnimationInstantly(GoblinAnimations.BURROW);
                float duration=GoblinAnimations.BURROW.get().getTotalTime();
                animator.getPlayerFor(GoblinAnimations.BURROW).setElapsedTime(
                    Math.min(Math.max(0,duration-.001F),mob.burrowProgress()*duration));
            }
        }else if(playing) {
            animator.playAnimation(GoblinAnimations.RECOVER,0);
        }
    }
    private GoblinBurrowPlayback() {}
}
