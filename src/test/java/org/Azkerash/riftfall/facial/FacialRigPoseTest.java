package org.Azkerash.riftfall.facial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FacialRigPoseTest {
    @Test void pupilsSlideWithinTheFlatWhitesAndStayTrackingDuringBlink(){
        var pose=FacialRigPose.from(new FaceFrame(FaceExpression.NEUTRAL,2,100,-100));
        assertEquals(.64F,pose.pupilX(),.0001);assertEquals(-.3F,pose.pupilY(),.0001);assertEquals(1,pose.lidClosure());
        assertTrue(Math.abs(pose.pupilX())+.3F<1.1F);assertTrue(Math.abs(pose.pupilY())+.5F<.9F);
    }
    @Test void expressionsControlActualBrowAndJawPivots(){
        var neutral=FacialRigPose.from(FaceFrame.NEUTRAL);
        for(var expression:FaceExpression.values())if(expression!=FaceExpression.NEUTRAL)
            assertNotEquals(neutral,FacialRigPose.from(new FaceFrame(expression,0,0,0)));
    }
    @Test void jawOverridesAndTalkPreviewLeaveTheEyesAndExpressionIndependent(){
        var animator=new FaceAnimator(FacialProfile.defaults(),7);animator.gaze(2,0);animator.blink(0);animator.expression(FaceExpression.ANGRY);
        animator.jaw(15);FaceFrame frame=null;for(int t=0;t<20;t++)frame=animator.update(t,FaceAnimator.Input.IDLE);
        assertEquals(.3F,FacialRigPose.from(frame).jawAngle(),.0001);assertEquals(100,frame.gazeX());
        animator.talking(true);var openings=new java.util.HashSet<Integer>();
        for(int t=20;t<40;t++){frame=animator.update(t,FaceAnimator.Input.IDLE);openings.add(frame.jaw());assertEquals(FaceExpression.ANGRY,frame.expression());assertEquals(100,frame.gazeX());}
        assertTrue(openings.size()>4);animator.reset();assertEquals(-1,animator.update(41,FaceAnimator.Input.IDLE).jaw());
    }
    @Test void fineGazeAndJawSurviveSynchronization(){
        for(var expression:FaceExpression.values())for(int blink=0;blink<3;blink++)for(int x=-100;x<=100;x+=13)for(int jaw=-1;jaw<16;jaw++){
            var frame=new FaceFrame(expression,blink,x,-x,jaw);assertEquals(frame,FaceFrame.unpack(frame.pack()));
        }
    }
}
