package org.Azkerash.riftfall;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Shared mineral geometry for the mesh, selection bounds, and growth-stage checks. */
public final class CrystalFormation {
    public record Prism(Vec3 root, Vec3 tip, double radius) {
        public Vec3 vertex(int corner, boolean shoulder) {
            Vec3 axis=tip.subtract(root).normalize();
            Vec3 side=axis.cross(new Vec3(0,0,1)).normalize();
            if (side.lengthSqr()<.01) side=axis.cross(new Vec3(1,0,0)).normalize();
            Vec3 other=axis.cross(side).normalize();
            double angle=corner*Math.PI/3;
            Vec3 center=shoulder ? root.lerp(tip,.78) : root;
            return center.add(side.scale(Math.cos(angle)*radius)).add(other.scale(Math.sin(angle)*radius));
        }
    }
    public static List<Prism> crystals(int count,int variant) {
        if (count<1 || count>5 || variant<0 || variant>2) throw new IllegalArgumentException("Invalid crystal formation");
        double jitter=(variant-1)*.45;
        List<Prism> result=new ArrayList<>();
        Vec3 stem=new Vec3(7.7+jitter,.45,8);
        result.add(new Prism(stem,new Vec3(7.1-jitter,3+count*1.85+(variant-1)*.6,8.3+jitter),.9+count*.13));
        if (count>=2) result.add(new Prism(new Vec3(7.5,1.4,8),new Vec3(2.7+jitter,7.4+variant*.4,6.8),1.15));
        if (count>=3) result.add(new Prism(new Vec3(8,2.1,8.1),new Vec3(12.9,9.5-variant*.5,9.8+jitter),1.3));
        if (count>=4) result.add(new Prism(new Vec3(7.8,1.3,8.5),new Vec3(6.5-jitter,6.5+variant*.6,13),1.05));
        if (count>=5) result.add(new Prism(new Vec3(8.1,2.7,7.6),new Vec3(9.3+jitter,10.5,3.5-jitter),1.15));
        return result;
    }
    /** Clockwise outline. Connected edges share exactly matching endpoints. */
    public static List<Vec3> bed(int mask) {
        if (mask<0 || mask>15) throw new IllegalArgumentException("Invalid bed mask");
        List<Vec3> points=new ArrayList<>();
        points.add(new Vec3(3, .15, 5.3));
        points.add(new Vec3(4.3, .3, 3.8));
        points.add(new Vec3((mask&1)!=0 ? 5 : 6,.15,(mask&1)!=0 ? 0 : 2.9));
        points.add(new Vec3((mask&1)!=0 ? 11 : 9.9,.15,(mask&1)!=0 ? 0 : 3.5));
        points.add(new Vec3(12.7,.3,4.4));
        points.add(new Vec3(12,.15,5.4));
        points.add(new Vec3((mask&2)!=0 ? 16 : 13.2,.15,(mask&2)!=0 ? 5 : 6.4));
        points.add(new Vec3((mask&2)!=0 ? 16 : 12.8,.15,(mask&2)!=0 ? 11 : 9.8));
        points.add(new Vec3(11.5,.3,12.2));
        points.add(new Vec3(10,.15,11.8));
        points.add(new Vec3((mask&4)!=0 ? 11 : 9.3,.15,(mask&4)!=0 ? 16 : 13.3));
        points.add(new Vec3((mask&4)!=0 ? 5 : 6.1,.15,(mask&4)!=0 ? 16 : 12.5));
        points.add(new Vec3(3.9,.3,11.7));
        points.add(new Vec3(4.4,.15,10.1));
        points.add(new Vec3((mask&8)!=0 ? 0 : 2.8,.15,(mask&8)!=0 ? 11 : 9.5));
        points.add(new Vec3((mask&8)!=0 ? 0 : 3.3,.15,(mask&8)!=0 ? 5 : 6.4));
        return points;
    }
    public static Vec3 orient(Vec3 p,net.minecraft.core.Direction facing) {
        return switch (facing) {
            case UP -> p;
            case DOWN -> new Vec3(p.x,16-p.y,16-p.z);
            case NORTH -> new Vec3(p.x,p.z,16-p.y);
            case SOUTH -> new Vec3(p.x,16-p.z,p.y);
            case WEST -> new Vec3(16-p.y,p.z,16-p.x);
            case EAST -> new Vec3(p.y,p.z,p.x);
        };
    }
    private CrystalFormation() {}
}
