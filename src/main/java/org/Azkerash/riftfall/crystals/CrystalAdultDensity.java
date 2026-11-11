package org.Azkerash.riftfall.crystals;

import java.util.function.Predicate;
import net.minecraft.core.Direction;

/** Adult-only, same-tier packing. Local edges are north/east/south/west on the growth plane. */
public final class CrystalAdultDensity {
    public record Profile(int neighbors,int corners,double xScale,double zScale,double xShift,double zShift){}
    public static Profile profile(Direction face,Predicate<CrystalSurfaceConnections.Offset> matchingAdult){
        int mask=0,corners=0;
        for(int i=0;i<4;i++){var d=CrystalSurfaceConnections.tangent(face,i);if(matchingAdult.test(new CrystalSurfaceConnections.Offset(d.getStepX(),d.getStepY(),d.getStepZ())))mask|=1<<i;}
        for(int i=0;i<4;i++)if((mask&(1<<i))!=0&&(mask&(1<<((i+1)%4)))!=0){
            var a=CrystalSurfaceConnections.tangent(face,i);var b=CrystalSurfaceConnections.tangent(face,(i+1)%4);
            if(matchingAdult.test(new CrystalSurfaceConnections.Offset(a.getStepX()+b.getStepX(),a.getStepY()+b.getStepY(),a.getStepZ()+b.getStepZ())))corners++;
        }
        int n=Integer.bitCount(mask),x=Integer.bitCount(mask&10),z=Integer.bitCount(mask&5);
        return new Profile(n,corners,Math.min(2.05,1+.12*n+.14*x+.10*corners),Math.min(2.05,1+.12*n+.14*z+.10*corners),(((mask&2)!=0?1:0)-((mask&8)!=0?1:0))*.035,(((mask&4)!=0?1:0)-((mask&1)!=0?1:0))*.035);
    }
    /** An affine fit for a complete prism: no per-vertex clipping or collapsed faces. */
    public static double[] fit(double min,double max,double scale,double shift){
        double width=Math.min(1,(max-min)*scale),center=.5+((min+max)/2-.5)*scale+shift;
        double start=Math.clamp(center-width/2,0,1-width);return new double[]{start,start+width};
    }
    private CrystalAdultDensity(){}
}
