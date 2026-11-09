package org.Azkerash.riftfall;

import yesman.epicfight.api.animation.Pose;

/** Small local jitters, never a displacement of the body or a camera shake. */
final class GoblinCombatTremor {
    static float multiplier(float confidence) {
        return 2F-1.65F*Math.clamp(confidence,0F,1F);
    }
    static void apply(Pose pose,float seconds,int entityId,boolean attacking,float confidence) {
        float phase=entityId*1.618F;
        float fear=multiplier(confidence);
        float strength=(attacking ? .65F : 1F)*fear;
        rotate(pose,"Head",wave(seconds,phase,29)*1.1F*fear,wave(seconds,phase+2,37)*1.4F*fear,wave(seconds,phase+4,23)*.9F*fear);
        for (int side=0;side<2;side++) {
            String hand=side==0 ? "R" : "L";
            float offset=phase+side*3.7F;
            rotate(pose,"Arm_"+hand,wave(seconds,offset,41)*.35F*strength,0,wave(seconds,offset+1,31)*.3F*strength);
            rotate(pose,"Hand_"+hand,wave(seconds,offset+2,47)*.3F*strength,0,0);
            // Twist around the shin's long axis; the ankle stays planted.
            rotate(pose,"Leg_"+hand,0,wave(seconds,offset+3,33)*.25F*fear,0);
        }
    }
    private static float wave(float time,float phase,float frequency) {
        return (float)(Math.sin(time*frequency+phase)*.7+Math.sin(time*frequency*1.73+phase*2)*.3);
    }
    private static void rotate(Pose pose,String joint,float x,float y,float z) {
        var transform=pose.orElseEmpty(joint);
        float radians=(float)(Math.PI/180);
        transform.rotation().rotateXYZ(x*radians,y*radians,z*radians);
        pose.putJointData(joint,transform);
    }
    private GoblinCombatTremor() {}
}
