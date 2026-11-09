package org.Azkerash.riftfall.facial;

/** Species-independent presentation state: gaze -100..100, blink 0..2, jaw -1 expression/0..15 override. */
public record FaceFrame(FaceExpression expression,int blink,int gazeX,int gazeY,int jaw) {
    public FaceFrame(FaceExpression expression,int blink,int gazeX,int gazeY){this(expression,blink,gazeX,gazeY,-1);}
    public static final FaceFrame NEUTRAL=new FaceFrame(FaceExpression.NEUTRAL,0,0,0);
    public FaceFrame {
        java.util.Objects.requireNonNull(expression);
        if(blink<0 || blink>2 || Math.abs((long)gazeX)>100 || Math.abs((long)gazeY)>100 || jaw< -1 || jaw>15) throw new IllegalArgumentException("Invalid facial frame");
    }
    public int pack(){return expression.ordinal() | blink<<3 | (gazeX+100)<<5 | (gazeY+100)<<13 | (jaw+1)<<21;}
    public static FaceFrame unpack(int value){
        int expression=value&7,blink=(value>>>3)&3,x=((value>>>5)&255)-100,y=((value>>>13)&255)-100,jaw=((value>>>21)&31)-1;
        if(value<0 || (value>>>26)!=0 || expression>=FaceExpression.values().length || blink>2 || x>100 || y>100 || jaw>15)return NEUTRAL;
        return new FaceFrame(FaceExpression.values()[expression],blink,x,y,jaw);
    }
}
