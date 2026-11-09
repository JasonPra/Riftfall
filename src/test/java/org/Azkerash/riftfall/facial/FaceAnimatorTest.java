package org.Azkerash.riftfall.facial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FaceAnimatorTest {
    @Test void acquisitionSurprisesThenSettlesIntoCombatWithoutFlickeringOnBriefOcclusion() {
        var animator=new FaceAnimator(FacialProfile.defaults(),1);var target=new FaceAnimator.Input(4,0,0);
        assertEquals(FaceExpression.SURPRISED,animator.update(0,target).expression());
        assertEquals(FaceExpression.ANGRY,animator.update(20,target).expression());
        animator.update(21,FaceAnimator.Input.IDLE);
        assertEquals(FaceExpression.ANGRY,animator.update(22,target).expression());
        assertEquals(FaceExpression.SURPRISED,animator.update(23,new FaceAnimator.Input(5,0,0)).expression());
    }
    @Test void tracksRelativeHeadYawAndPitchAndReversesWhenTargetMoves() {
        var animator=new FaceAnimator(FacialProfile.defaults(),2);animator.blink(0);FaceFrame frame=null;
        for (int t=0;t<10;t++) frame=animator.update(t,new FaceAnimator.Input(4,35,-25));
        assertEquals(100,frame.gazeX(),1);assertEquals(-100,frame.gazeY(),1);
        for (int t=10;t<20;t++) frame=animator.update(t,new FaceAnimator.Input(4,-35,25));
        assertEquals(-100,frame.gazeX(),1);assertEquals(100,frame.gazeY(),1);
    }
    @Test void idleGlancesWanderWithoutRequiringATarget() {
        var animator=new FaceAnimator(FacialProfile.defaults(),3);animator.blink(0);
        var poses=new java.util.HashSet<String>();
        for (int tick=0;tick<1000;tick++) { var frame=animator.update(tick,FaceAnimator.Input.IDLE);poses.add(frame.gazeX()+":"+frame.gazeY());assertEquals(FaceExpression.NEUTRAL,frame.expression()); }
        assertTrue(poses.size()>3);
    }
    @Test void differentSeedsCannotLeaveAnIdleFaceStaringAtCenterIndefinitely() {
        for (int seed=0;seed<100;seed++) {
            var animator=new FaceAnimator(FacialProfile.defaults(),seed);animator.blink(0);
            boolean moved=false,centered=false;
            for (int tick=0;tick<200;tick++) {
                var frame=animator.update(tick,FaceAnimator.Input.IDLE);
                if (frame.gazeX()!=0 || frame.gazeY()!=0) moved=true;else centered=true;
            }
            assertTrue(moved && centered,"Missing glance/rest phases for seed "+seed);
        }
    }
    @Test void blinkUsesHalfClosedHalfOpenSequenceAndNeverStaysShut() {
        var animator=new FaceAnimator(FacialProfile.defaults(),4);animator.blinkOnce(0);
        assertEquals(1,animator.update(0,FaceAnimator.Input.IDLE).blink());
        assertEquals(2,animator.update(1,FaceAnimator.Input.IDLE).blink());
        assertEquals(2,animator.update(2,FaceAnimator.Input.IDLE).blink());
        assertEquals(1,animator.update(3,FaceAnimator.Input.IDLE).blink());
        assertEquals(0,animator.update(4,FaceAnimator.Input.IDLE).blink());
    }
    @Test void manualExpressionsAndEyeOverridesDoNotAffectBehaviorAndCanBeReset() {
        var animator=new FaceAnimator(FacialProfile.defaults(),5);animator.expression(FaceExpression.SCARED);animator.gaze(-2,1);animator.blink(0);
        FaceFrame frame=null;for (int t=0;t<10;t++) frame=animator.update(t,new FaceAnimator.Input(4,35,-25));
        assertEquals(FaceExpression.SCARED,frame.expression());assertEquals(-100,frame.gazeX(),1);assertEquals(100,frame.gazeY(),1);
        animator.reset();assertEquals(FaceExpression.ANGRY,animator.update(30,new FaceAnimator.Input(4,0,0)).expression());
    }
    @Test void anotherSpeciesCanSupplyItsOwnTimingAndCombatPresentation() {
        var profile=new FacialProfile(100,100,10,10,0,10,10,1,FaceExpression.UNCERTAIN,FaceExpression.SCARED);
        var animator=new FaceAnimator(profile,6);
        assertEquals(FaceExpression.UNCERTAIN,animator.update(0,FaceAnimator.Input.IDLE).expression());
        var frame=animator.update(1,new FaceAnimator.Input(4,10,10));
        assertEquals(FaceExpression.SCARED,frame.expression());assertEquals(100,frame.gazeX(),1);assertEquals(100,frame.gazeY(),1);
    }
    @Test void everyPresentationFrameRoundTripsIncludingGazeBehindClosedLids() {
        var frames=new java.util.HashSet<Integer>();
        for (var expression:FaceExpression.values()) for (int blink=0;blink<3;blink++) for (int x=-2;x<=2;x++) for (int y=-1;y<=1;y++) {
            var frame=new FaceFrame(expression,blink,x,y);assertEquals(frame,FaceFrame.unpack(frame.pack()));frames.add(frame.pack());
        }
        assertEquals(225,frames.size());assertEquals(FaceFrame.NEUTRAL,FaceFrame.unpack(-1));
    }
}
