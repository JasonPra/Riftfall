package org.Azkerash.riftfall;

import org.junit.jupiter.api.Test;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.JointTransform;
import static org.junit.jupiter.api.Assertions.*;

class GoblinCombatTremorTest {
    @Test void tremorIsLocalToTheHeadAndLimbs() {
        Pose pose=new Pose();
        for (String joint : new String[]{"Root","Torso","Chest","Head","Arm_R","Arm_L","Leg_R","Leg_L"})
            pose.putJointData(joint,JointTransform.empty());
        var root=pose.orElseEmpty("Root").copy(); var torso=pose.orElseEmpty("Torso").copy(); var chest=pose.orElseEmpty("Chest").copy();
        GoblinCombatTremor.apply(pose,1.3F,17,true);
        assertEquals(root.rotation(),pose.orElseEmpty("Root").rotation());
        assertEquals(torso.rotation(),pose.orElseEmpty("Torso").rotation());
        assertEquals(chest.rotation(),pose.orElseEmpty("Chest").rotation());
        assertNotEquals(JointTransform.empty().rotation(),pose.orElseEmpty("Head").rotation());
        assertNotEquals(JointTransform.empty().rotation(),pose.orElseEmpty("Arm_R").rotation());
        for (String joint : new String[]{"Root","Torso","Chest","Head","Arm_R","Arm_L","Hand_R","Hand_L","Leg_R","Leg_L"})
            assertEquals(0,pose.orElseEmpty(joint).translation().length());
    }
    @Test void nearbyGoblinsDoNotTrembleInUnison() {
        Pose first=new Pose(),second=new Pose(),later=new Pose();
        GoblinCombatTremor.apply(first,1.3F,17,true);
        GoblinCombatTremor.apply(second,1.3F,18,true);
        GoblinCombatTremor.apply(later,1.35F,17,true);
        assertNotEquals(first.orElseEmpty("Head").rotation(),second.orElseEmpty("Head").rotation());
        assertNotEquals(first.orElseEmpty("Head").rotation(),later.orElseEmpty("Head").rotation());
    }
}
