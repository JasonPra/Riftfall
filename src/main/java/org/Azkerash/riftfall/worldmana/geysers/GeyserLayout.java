package org.Azkerash.riftfall.worldmana.geysers;

import java.util.Optional;

/** Stateless regional generation. Each lookup examines one cell, never neighboring chunks. */
public final class GeyserLayout {
    public static final int CELL_CHUNKS=512,INSET_CHUNKS=160;
    public static final int SMALL_WIDTH=129,MEDIUM_WIDTH=193,LARGE_WIDTH=257;
    public static final double SMALL_OUTPUT=SMALL_WIDTH*SMALL_WIDTH*5.0,MEDIUM_OUTPUT=MEDIUM_WIDTH*MEDIUM_WIDTH*7.5,LARGE_OUTPUT=LARGE_WIDTH*LARGE_WIDTH*10.0;
    private final long seed;
    public GeyserLayout(long seed){this.seed=seed;}
    public long seed(){return seed;}
    public static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    public Optional<GeyserRegion> cell(int cellX,int cellZ){
        long hash=mix(seed^cellX*0x632be59bd9b4e019L^cellZ*0x9e3779b97f4a7c15L);
        if((hash&1)!=0)return Optional.empty(); // Half of 512x512-chunk cells contain a field.
        long x=(long)cellX*CELL_CHUNKS+INSET_CHUNKS+Math.floorMod(mix(hash+1),CELL_CHUNKS-INSET_CHUNKS*2);
        long z=(long)cellZ*CELL_CHUNKS+INSET_CHUNKS+Math.floorMod(mix(hash+2),CELL_CHUNKS-INSET_CHUNKS*2);
        int kind=(int)(mix(hash+3)&7); // 50% small, 37.5% medium, 12.5% large.
        int width=kind<4?SMALL_WIDTH:kind<7?MEDIUM_WIDTH:LARGE_WIDTH;
        double output=kind<4?SMALL_OUTPUT:kind<7?MEDIUM_OUTPUT:LARGE_OUTPUT;
        long limit=1_875_000L-width/2-1;
        if(Math.abs(x)>limit||Math.abs(z)>limit)return Optional.empty();
        return Optional.of(new GeyserRegion((int)x,(int)z,width,output));
    }
    public Optional<GeyserRegion> at(int chunkX,int chunkZ){return cell(Math.floorDiv(chunkX,CELL_CHUNKS),Math.floorDiv(chunkZ,CELL_CHUNKS)).filter(g->g.contains(chunkX,chunkZ));}
}
