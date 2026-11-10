package org.Azkerash.riftfall;

import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.behavior.TargetAwareness;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetAwarenessTest {
    @Test void attentionUsesHorizontalFacingAndRequiresARearOpening() {
        var forward=new Vec3(0,0,1);
        assertTrue(TargetAwareness.facingAway(forward,new Vec3(0,0,-8)));
        assertFalse(TargetAwareness.facingAway(forward,new Vec3(0,0,8)));
        assertFalse(TargetAwareness.facingAway(forward,new Vec3(8,0,0)));
        assertTrue(TargetAwareness.facingAway(new Vec3(0,.9,.1),new Vec3(0,4,-8)));
        assertFalse(TargetAwareness.facingAway(new Vec3(0,1,0),new Vec3(0,0,-8)));
        assertFalse(TargetAwareness.facingAway(forward,Vec3.ZERO));
    }
}
