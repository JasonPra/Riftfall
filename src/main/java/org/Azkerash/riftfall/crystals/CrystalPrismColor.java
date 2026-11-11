package org.Azkerash.riftfall.crystals;

/** A stable, whole-prism mineral choice; never interpolates colors or adds geometry. */
public final class CrystalPrismColor {
    public static CrystalTier choose(CrystalTier own, CrystalTier[] neighbors, double[] bounds, long position, int piece) {
        long grain=mix(position ^ ((long)piece*0x9e3779b97f4a7c15L));
        // Leave half of boundary pieces in their native tier. All faces share this decision.
        if((grain&1)==0 || bounds==null)return own;
        double[] distance={bounds[1],1-bounds[4],bounds[2],1-bounds[5],bounds[0],1-bounds[3]};
        CrystalTier selected=own;long best=0;boolean found=false;
        for(int side=0;side<6;side++){
            var other=neighbors[side];
            if(other==null||other==own||distance[side]>=CrystalEdgeBlend.WIDTH)continue;
            long score=mix(grain+side*0x632be59bd9b4e019L);
            if(!found||Long.compareUnsigned(score,best)>0){selected=other;best=score;found=true;}
        }
        return selected;
    }
    private static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    private CrystalPrismColor(){}
}
