package org.Azkerash.riftfall;

import org.junit.jupiter.api.Test;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import static org.junit.jupiter.api.Assertions.*;

class WeaponColliderTest {
    private static OBBCollider box(double x,double y,double z,double cx,double cy,double cz) {
        var collider=new OBBCollider(x,y,z,cx,cy,cz);
        collider.transform(new OpenMatrix4f());return collider;
    }
    @Test void equallyCloseTargetsOnlyTakeContactOnTheWeaponSide() {
        var blade=box(.06,.06,.6,0,0,-.6);
        assertTrue(blade.isCollide(box(.2,.4,.2,0,0,-.8)));
        assertFalse(blade.isCollide(box(.2,.4,.2,.8,0,0)));
    }
    @Test void movingOutsideTheBladeMissesDespiteRemainingNearby() {
        var blade=box(.06,.06,.6,0,0,-.6);
        assertFalse(blade.isCollide(box(.1,.1,.1,.3,0,-.8)));
        assertFalse(blade.isCollide(box(.1,.1,.1,0,.3,-.8)));
    }
    @Test void rotatingTheWeaponChangesWhoItContacts() {
        var blade=box(.06,.06,.6,0,0,-.6);
        blade.transform(new OpenMatrix4f().rotateDeg(90,Vec3f.Y_AXIS));
        assertFalse(blade.isCollide(box(.1,.1,.1,0,0,-.8)));
        assertTrue(blade.isCollide(box(.1,.1,.1,-.8,0,0)));
    }
    @Test void staffFacingTurnsTheBodyTowardsEveryTargetDirection() {
        for (double[] target:new double[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1}}) {
            double yaw=Math.toRadians(GoblinTactics.staffFacing(target[0],target[1]));
            double length=Math.hypot(target[0],target[1]);
            assertEquals(1,(-Math.sin(yaw)*target[0]+Math.cos(yaw)*target[1])/length,.00001);
        }
    }
}
