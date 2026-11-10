package org.Azkerash.riftfall.facial;

/** Timing and eye range can differ by species without changing the animator. Tick units are game ticks. */
public record FacialProfile(int blinkMin,int blinkMax,int glanceMin,int glanceMax,int surpriseTicks,
        double yawRange,double pitchRange,double smoothing,FaceExpression idle,FaceExpression combat,
        int blinkHalfTicks,int blinkClosedTicks) {
    public FacialProfile(int blinkMin,int blinkMax,int glanceMin,int glanceMax,int surpriseTicks,
            double yawRange,double pitchRange,double smoothing,FaceExpression idle,FaceExpression combat) {
        this(blinkMin,blinkMax,glanceMin,glanceMax,surpriseTicks,yawRange,pitchRange,smoothing,idle,combat,1,2);
    }
    public FacialProfile {
        if (blinkHalfTicks<1 || blinkClosedTicks<1 || blinkMin<=2L*blinkHalfTicks+blinkClosedTicks || blinkMin<5 || blinkMax<blinkMin || glanceMin<1 || glanceMax<glanceMin || surpriseTicks<0
                || !Double.isFinite(yawRange) || yawRange<=0 || !Double.isFinite(pitchRange) || pitchRange<=0
                || !Double.isFinite(smoothing) || smoothing<=0 || smoothing>1) throw new IllegalArgumentException("Invalid facial profile");
        java.util.Objects.requireNonNull(idle);java.util.Objects.requireNonNull(combat);
    }
    public FacialProfile withBlinkTiming(int halfTicks,int closedTicks) {
        return new FacialProfile(blinkMin,blinkMax,glanceMin,glanceMax,surpriseTicks,yawRange,pitchRange,smoothing,idle,combat,halfTicks,closedTicks);
    }
    public static FacialProfile defaults() { return new FacialProfile(60,160,35,90,20,35,25,.4,FaceExpression.NEUTRAL,FaceExpression.ANGRY); }
}
