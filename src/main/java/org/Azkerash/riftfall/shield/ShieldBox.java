package org.Azkerash.riftfall.shield;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

/** Model-local bounds transformed into Epic Fight's collision coordinate system. */
public final class ShieldBox {
    private final AABB local;
    private final OpenMatrix4f matrix,inverse;
    private final OBBCollider collider;
    public ShieldBox(AABB local,OpenMatrix4f matrix) {
        this.local=local;this.matrix=new OpenMatrix4f(matrix);inverse=OpenMatrix4f.invert(matrix,null);
        var center=local.getCenter();
        collider=new OBBCollider(local.getXsize()/2,local.getYsize()/2,local.getZsize()/2,center.x,center.y,center.z);
        collider.transform(new OpenMatrix4f(matrix));
    }
    public OBBCollider collider(){return collider;}
    public Vec3 center(){return toWorld(OpenMatrix4f.transform(matrix,local.getCenter()));}
    public static Vec3 toWorld(Vec3 epic){return new Vec3(-epic.x,epic.y,-epic.z);}
    public static Vec3 toEpic(Vec3 world){return new Vec3(-world.x,world.y,-world.z);}
    public boolean triangle(Vec3 a,Vec3 b,Vec3 c) {
        var center=local.getCenter();
        Vec3[] p={OpenMatrix4f.transform(inverse,toEpic(a)).subtract(center),OpenMatrix4f.transform(inverse,toEpic(b)).subtract(center),OpenMatrix4f.transform(inverse,toEpic(c)).subtract(center)};
        Vec3[] edges={p[1].subtract(p[0]),p[2].subtract(p[1]),p[0].subtract(p[2])};
        var axes=new java.util.ArrayList<Vec3>();axes.add(new Vec3(1,0,0));axes.add(new Vec3(0,1,0));axes.add(new Vec3(0,0,1));
        axes.add(edges[0].cross(edges[1]));for(var edge:edges)for(int i=0;i<3;i++)axes.add(edge.cross(axes.get(i)));
        for(var axis:axes) {
            if(axis.lengthSqr()<1E-12)continue;
            double radius=(local.getXsize()*Math.abs(axis.x)+local.getYsize()*Math.abs(axis.y)+local.getZsize()*Math.abs(axis.z))/2;
            double min=Math.min(p[0].dot(axis),Math.min(p[1].dot(axis),p[2].dot(axis))),max=Math.max(p[0].dot(axis),Math.max(p[1].dot(axis),p[2].dot(axis)));
            if(min>radius+1E-8 || max< -radius-1E-8)return false;
        }
        return true;
    }
    public Vec3[] corners() {
        Vec3[] points=new Vec3[8];
        for(int i=0;i<8;i++)points[i]=toWorld(OpenMatrix4f.transform(matrix,new Vec3((i&1)==0?local.minX:local.maxX,(i&2)==0?local.minY:local.maxY,(i&4)==0?local.minZ:local.maxZ)));
        return points;
    }
    /** Segment entry parameter; infinity means the actual shield box was missed. */
    public double entry(Vec3 start,Vec3 end,double radius) {
        var a=OpenMatrix4f.transform(inverse,toEpic(start));var b=OpenMatrix4f.transform(inverse,toEpic(end));
        var bounds=local.inflate(radius);double low=0,high=1;
        double[] origin={a.x,a.y,a.z},delta={b.x-a.x,b.y-a.y,b.z-a.z},min={bounds.minX,bounds.minY,bounds.minZ},max={bounds.maxX,bounds.maxY,bounds.maxZ};
        for(int axis=0;axis<3;axis++) {
            if(Math.abs(delta[axis])<1.0E-9){if(origin[axis]<min[axis]||origin[axis]>max[axis])return Double.POSITIVE_INFINITY;}
            else {
                double first=(min[axis]-origin[axis])/delta[axis],last=(max[axis]-origin[axis])/delta[axis];
                low=Math.max(low,Math.min(first,last));high=Math.min(high,Math.max(first,last));
                if(low>high)return Double.POSITIVE_INFINITY;
            }
        }
        return low;
    }
    /** A rear strike must reach the body first, rather than block on a shield behind the body. */
    public boolean beforeBody(Vec3 start,Vec3 end,AABB body,double radius) {
        double shield=entry(start,end,radius);if(!Double.isFinite(shield))return false;
        var hit=body.clip(start,end);
        if(hit.isEmpty())return !body.contains(start);
        return !body.contains(start) && start.distanceToSqr(hit.get())+1.0E-6>=start.distanceToSqr(start.lerp(end,shield));
    }
}
