package org.Azkerash.riftfall.crystals;

/** Deterministic mineral color mixing in a four-pixel band. Does not change tiers or inventory data. */
public final class CrystalEdgeBlend {
    public static final double WIDTH=.25;
    /** Neighbors use Minecraft's direction order: down, up, north, south, west, east. Null means no crystal. */
    public static int color(CrystalTier own,CrystalTier[] neighbors,double x,double y,double z,double worldX,double worldY,double worldZ){
        if(neighbors.length!=6)throw new IllegalArgumentException("Six neighbors required");
        double[] distance={y,1-y,z,1-z,x,1-x};double[] sums=new double[3];double weight=0;
        long grain=mix((long)Math.floor(worldX*8+1e-5)*73428767L^(long)Math.floor(worldY*8+1e-5)*912931L^(long)Math.floor(worldZ*8+1e-5)*438289L);
        double patch=((grain>>>11)*0x1.0p-53-.5)*.36;
        for(int side=0;side<6;side++){
            var other=neighbors[side];if(other==null||other==own||distance[side]>=WIDTH)continue;
            double amount=Math.clamp(1-distance[side]/WIDTH,0,1);amount=amount*amount*(3-2*amount);
            // Both sides of a seam agree on its grain color, even though their own/other roles swap.
            double toward=.5+patch*(own.ordinal()<other.ordinal()?1:-1);
            for(int c=0;c<3;c++){int shift=(2-c)*8;double a=(own.color()>>shift)&255,b=(other.color()>>shift)&255;sums[c]+=(a+(b-a)*toward)*amount;}
            weight+=amount;
        }
        int result=0;for(int c=0;c<3;c++){int shift=(2-c)*8;double a=(own.color()>>shift)&255;double value=weight>1?sums[c]/weight:a*(1-weight)+sums[c];result|=(int)Math.round(value)<<shift;}return result;
    }
    private static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    private CrystalEdgeBlend(){}
}
