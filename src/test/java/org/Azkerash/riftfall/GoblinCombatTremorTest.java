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
        GoblinCombatTremor.apply(pose,1.3F,17,true,.5F);
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
        GoblinCombatTremor.apply(first,1.3F,17,true,.5F);
        GoblinCombatTremor.apply(second,1.3F,18,true,.5F);
        GoblinCombatTremor.apply(later,1.35F,17,true,.5F);
        assertNotEquals(first.orElseEmpty("Head").rotation(),second.orElseEmpty("Head").rotation());
        assertNotEquals(first.orElseEmpty("Head").rotation(),later.orElseEmpty("Head").rotation());
    }
    @Test void confidenceContinuouslyReducesTheMultiplierWithinSafeBounds() {
        assertEquals(2F,GoblinCombatTremor.multiplier(0),.0001F);
        assertEquals(.35F,GoblinCombatTremor.multiplier(1),.0001F);
        assertEquals(2F,GoblinCombatTremor.multiplier(-1),.0001F);
        assertEquals(.35F,GoblinCombatTremor.multiplier(2),.0001F);
        for (int i=1;i<=100;i++)
            assertTrue(GoblinCombatTremor.multiplier(i/100F)<GoblinCombatTremor.multiplier((i-1)/100F));
    }
    @Test void lowConfidenceIncreasesHeadAndLimbTremorWithoutMovingTheBody() {
        Pose frightened=new Pose(),confident=new Pose();
        GoblinCombatTremor.apply(frightened,1.3F,17,true,0);
        GoblinCombatTremor.apply(confident,1.3F,17,true,1);
        for (String joint:new String[]{"Head","Arm_R","Hand_L","Leg_R"})
            assertTrue(frightened.orElseEmpty(joint).rotation().angle()>confident.orElseEmpty(joint).rotation().angle(),joint);
        for (String joint:new String[]{"Root","Torso","Chest"})
            assertEquals(JointTransform.empty().rotation(),frightened.orElseEmpty(joint).rotation());
    }

}
