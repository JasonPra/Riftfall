package org.Azkerash.riftfall.facial;

import java.util.Random;

/** Pure presentation state. No goblin checks, confidence/fear logic, pathfinding or world references. */
public final class FaceAnimator {
    public record Input(int targetId,double relativeYaw,double relativePitch) {
        public static final Input IDLE=new Input(-1,0,0);
        public Input { if (!Double.isFinite(relativeYaw) || !Double.isFinite(relativePitch)) throw new IllegalArgumentException("Invalid eye target"); }
    }
    private final FacialProfile profile;
    private final Random random;
    private long nextBlink=-1,blinkStart=Long.MIN_VALUE,nextGlance=-1,surprisedUntil,lastSeen=Long.MIN_VALUE;
    private int lastTarget=-1,idleX,idleY,forcedBlink=-1;
    private Integer forcedX,forcedY;
    private FaceExpression forcedExpression;
    private boolean cycle,talking;
    private int forcedJaw=-1;
    private double x,y;
    public FaceAnimator(FacialProfile profile,long seed) { this.profile=profile;random=new Random(seed); }
    private int interval(int min,int max) { return min+random.nextInt(max-min+1); }
    public FaceFrame update(long tick,Input input) { return update(tick,input,null); }
    public FaceFrame update(long tick,Input input,FaceExpression automaticExpression) {
        boolean tracking=input.targetId()>=0;
        if (tracking) {
            if (input.targetId()!=lastTarget) surprisedUntil=tick+profile.surpriseTicks();
            lastTarget=input.targetId();lastSeen=tick;
        } else if (lastSeen!=Long.MIN_VALUE && tick-lastSeen>40) lastTarget=-1;
        if (nextGlance<0 || tick>=nextGlance) {
            if (idleX==0 && idleY==0) {
                do { idleX=random.nextInt(5)-2;idleY=random.nextInt(3)-1; } while (idleX==0 && idleY==0);
            } else { idleX=0;idleY=0; }
            nextGlance=tick+interval(profile.glanceMin(),profile.glanceMax());
        }
        double wantedX=forcedX!=null?forcedX:tracking?Math.clamp(input.relativeYaw()/profile.yawRange(),-1,1)*2:idleX;
        double wantedY=forcedY!=null?forcedY:tracking?Math.clamp(input.relativePitch()/profile.pitchRange(),-1,1):idleY;
        x+=(wantedX-x)*profile.smoothing();y+=(wantedY-y)*profile.smoothing();
        if (nextBlink<0) nextBlink=tick+interval(profile.blinkMin(),profile.blinkMax());
        if (tick>=nextBlink) { blinkStart=tick;nextBlink=tick+interval(profile.blinkMin(),profile.blinkMax()); }
        long blinkAge=blinkStart==Long.MIN_VALUE?Long.MAX_VALUE:tick-blinkStart;
        long closeEnd=profile.blinkHalfTicks()+profile.blinkClosedTicks();
        long blinkEnd=closeEnd+profile.blinkHalfTicks();
        int blink=forcedBlink>=0?forcedBlink:blinkAge<0 || blinkAge>=blinkEnd?0:
            blinkAge<profile.blinkHalfTicks() || blinkAge>=closeEnd?1:2;
        FaceExpression expression=cycle?FaceExpression.values()[(int)(Math.floorDiv(tick,40)%FaceExpression.values().length)]:
            forcedExpression!=null?forcedExpression:automaticExpression!=null?automaticExpression:tracking?(tick<surprisedUntil?FaceExpression.SURPRISED:profile.combat()):profile.idle();
        return new FaceFrame(expression,blink,(int)Math.round(x*50),(int)Math.round(y*100),talking?(int)Math.round((Math.sin(tick*1.1)+1)*6):forcedJaw);
    }
    public void expression(FaceExpression expression) { forcedExpression=expression;cycle=false; }
    public void cycle() { cycle=true;forcedExpression=null; }
    public void blink(int stage) { if (stage< -1 || stage>2) throw new IllegalArgumentException("Invalid blink override");forcedBlink=stage; }
    public void blinkOnce(long tick) { forcedBlink=-1;nextBlink=tick; }
    public void gaze(Integer x,Integer y) {
        if ((x==null)!=(y==null) || x!=null && (x< -2 || x>2 || y< -1 || y>1)) throw new IllegalArgumentException("Invalid gaze override");
        forcedX=x;forcedY=y;
    }
    public void jaw(int amount) { if(amount< -1 || amount>15)throw new IllegalArgumentException("Invalid jaw opening");forcedJaw=amount;talking=false; }
    public void talking(boolean enabled) { talking=enabled; }
    public void reset() { forcedJaw=-1;talking=false; forcedExpression=null;cycle=false;forcedBlink=-1;forcedX=null;forcedY=null; }
}
