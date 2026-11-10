package org.Azkerash.riftfall.behavior;

import net.minecraft.world.phys.Vec3;

/** Horizontal attention geometry; callers still decide visibility, faction and behavior eligibility. */
public final class TargetAwareness {
    public static boolean facingAway(Vec3 look,Vec3 targetToObserver) {
        var facing=look.multiply(1,0,1);var offset=targetToObserver.multiply(1,0,1);
        if(facing.lengthSqr()<1E-8 || offset.lengthSqr()<1E-8)return false;
        return facing.normalize().dot(offset.normalize())<-.15;
    }
    private TargetAwareness(){}
}
