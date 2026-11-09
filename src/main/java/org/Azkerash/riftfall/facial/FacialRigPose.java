package org.Azkerash.riftfall.facial;

/** Geometry controls in model pixels/radians, independent of entity type or rendering API. */
public record FacialRigPose(float pupilX,float pupilY,float lidClosure,float browHeight,float browAngle,float jawAngle) {
    public static FacialRigPose from(FaceFrame frame) {
        float height=0,angle=0,jaw=0,closure=frame.blink()*.5F;
        switch(frame.expression()) {
            case ANGRY -> {angle=.28F;closure=Math.max(closure,.15F);}
            case SURPRISED -> {height=-.45F;jaw=.25F;}
            case SCARED -> {height=-.25F;angle=-.28F;jaw=.12F;}
            case UNCERTAIN -> {angle=-.16F;closure=Math.max(closure,.2F);}
            default -> {}
        }
        if(frame.jaw()>=0)jaw=frame.jaw()/15F*.3F;
        return new FacialRigPose(frame.gazeX()*.0064F,frame.gazeY()*.003F,closure,height,angle,jaw);
    }
}
