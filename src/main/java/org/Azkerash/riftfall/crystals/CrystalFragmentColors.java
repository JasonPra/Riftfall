package org.Azkerash.riftfall.crystals;

/** Stable, whole-prism material inheritance; no color interpolation and no per-frame randomness. */
public final class CrystalFragmentColors {
    public static final double EDGE_WIDTH=.25;
    public record Bounds(double minX,double minY,double minZ,double maxX,double maxY,double maxZ){}
    public static CrystalTier choose(CrystalTier own,CrystalTier[] neighbors,Bounds b,long seed){
        return choose(own,neighbors,b,seed,.25);
    }
    public static CrystalTier choose(CrystalTier own,CrystalTier[] neighbors,Bounds b,long seed,double chance){
        if(neighbors.length!=6)throw new IllegalArgumentException("Six neighbors required");
        double[] distance={b.maxY(),1-b.minY(),b.maxZ(),1-b.minZ(),b.maxX(),1-b.minX()};var eligible=new CrystalTier[6];int count=0;
        for(int side=0;side<6;side++)if(neighbors[side]!=null&&neighbors[side]!=own&&distance[side]<EDGE_WIDTH)eligible[count++]=neighbors[side];
        long choice=mix(seed);if(count==0||(choice>>>11)*0x1.0p-53>=chance)return own;
        return eligible[Math.floorMod((int)mix(choice+0x9e3779b97f4a7c15L),count)];
    }
    private static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    private CrystalFragmentColors(){}
}
