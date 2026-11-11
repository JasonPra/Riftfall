package org.Azkerash.riftfall.worldmana.geysers;

import java.util.Optional;

/** Stateless regional generation. Each lookup examines one cell, never neighboring chunks. */
public final class GeyserLayout {
    public static final int CELL_CHUNKS=128,INSET_CHUNKS=32;
    public static final int SMALL_WIDTH=9,LARGE_WIDTH=17;
    public static final double SMALL_OUTPUT=405,LARGE_OUTPUT=2890;
    private final long seed;
    public GeyserLayout(long seed){this.seed=seed;}
    public long seed(){return seed;}
    public static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    public Optional<GeyserRegion> cell(int cellX,int cellZ){
        long hash=mix(seed^cellX*0x632be59bd9b4e019L^cellZ*0x9e3779b97f4a7c15L);
        if((hash&1)!=0)return Optional.empty(); // Half of 128x128-chunk cells contain a field.
        long x=(long)cellX*CELL_CHUNKS+INSET_CHUNKS+Math.floorMod(mix(hash+1),CELL_CHUNKS-INSET_CHUNKS*2);
        long z=(long)cellZ*CELL_CHUNKS+INSET_CHUNKS+Math.floorMod(mix(hash+2),CELL_CHUNKS-INSET_CHUNKS*2);
        if(Math.abs(x)>1_874_960||Math.abs(z)>1_874_960)return Optional.empty();
        boolean large=(mix(hash+3)&3)==0; // One quarter of accepted fields are large.
        return Optional.of(new GeyserRegion((int)x,(int)z,large?LARGE_WIDTH:SMALL_WIDTH,large?LARGE_OUTPUT:SMALL_OUTPUT));
    }
    public Optional<GeyserRegion> at(int chunkX,int chunkZ){return cell(Math.floorDiv(chunkX,CELL_CHUNKS),Math.floorDiv(chunkZ,CELL_CHUNKS)).filter(g->g.contains(chunkX,chunkZ));}
}
