package org.Azkerash.riftfall.worldmana;

import java.util.Optional;

/** Bounded, generation-order-independent placement: one candidate per coarse cell, then local priority exclusion. */
public final class LeyLinePlacement {
    public static final int EXCLUSION_RADIUS=63;
    public record Candidate(int chunkX,int chunkZ,long priority){
        public double x(){return chunkX*16.0+8;}
        public double z(){return chunkZ*16.0+8;}
    }
    public static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    private static Candidate candidate(long seed,int cellX,int cellZ){
        long hash=mix(seed^cellX*0x632be59bd9b4e019L^cellZ*0x9e3779b97f4a7c15L);
        if((hash&3)!=0)return null; // 25% of 63x63-chunk cells receive a candidate, not 25% of chunks.
        long x=(long)cellX*EXCLUSION_RADIUS+Math.floorMod(mix(hash+1),EXCLUSION_RADIUS);
        long z=(long)cellZ*EXCLUSION_RADIUS+Math.floorMod(mix(hash+2),EXCLUSION_RADIUS);
        if(x<Integer.MIN_VALUE||x>Integer.MAX_VALUE||z<Integer.MIN_VALUE||z>Integer.MAX_VALUE)return null;
        return new Candidate((int)x,(int)z,mix(hash+3));
    }
    public static Optional<Candidate> at(long seed,int chunkX,int chunkZ){
        int cellX=Math.floorDiv(chunkX,EXCLUSION_RADIUS),cellZ=Math.floorDiv(chunkZ,EXCLUSION_RADIUS);
        Candidate own=candidate(seed,cellX,cellZ);if(own==null||own.chunkX()!=chunkX||own.chunkZ()!=chunkZ)return Optional.empty();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            if(dx==0&&dz==0)continue;Candidate neighbor=candidate(seed,cellX+dx,cellZ+dz);if(neighbor==null)continue;
            long x=(long)own.chunkX()-neighbor.chunkX(),z=(long)own.chunkZ()-neighbor.chunkZ();
            if(x*x+z*z<=(long)EXCLUSION_RADIUS*EXCLUSION_RADIUS&&wins(neighbor,own))return Optional.empty();
        }
        return Optional.of(own);
    }
    private static boolean wins(Candidate a,Candidate b){int rank=Long.compareUnsigned(a.priority(),b.priority());return rank<0||rank==0&&(a.chunkX()<b.chunkX()||a.chunkX()==b.chunkX()&&a.chunkZ()<b.chunkZ());}
    /** Useful for diagnostics without loading terrain. Returns a cell's accepted point, if any. */
    public static Optional<Candidate> inCell(long seed,int cellX,int cellZ){var c=candidate(seed,cellX,cellZ);return c==null?Optional.empty():at(seed,c.chunkX(),c.chunkZ());}
    private LeyLinePlacement(){}
}
