package org.Azkerash.riftfall.behavior;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Confidence may override enemy proximity, never the navigator's physical reachability checks. */
public final class HealingRouteSafety {
    public static boolean safe(Vec3 start,List<Vec3> route,Vec3 circle,List<Vec3> threats,double clearance,boolean confident) {
        if (confident) return true;
        for (var threat:threats) {
            if (circle.distanceTo(threat)<clearance) return false;
            double furthest=start.distanceTo(threat);Vec3 previous=start;
            for (var node:route) {
                int samples=Math.max(1,(int)Math.ceil(previous.distanceTo(node)*4));
                for (int i=1;i<=samples;i++) {
                    double distance=previous.lerp(node,(double)i/samples).distanceTo(threat);
                    double required=Math.min(clearance,Math.max(0,furthest-.35));
                    if (distance<required) return false;
                    furthest=Math.max(furthest,distance);
                }
                previous=node;
            }
        }
        return true;
    }
    private HealingRouteSafety() {}
}
