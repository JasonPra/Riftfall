package org.Azkerash.riftfall.worldmana.geysers;

/** A horizontal footprint spanning every build-height layer. There is deliberately no origin Y/depth. */
public record GeyserRegion(int centerChunkX,int centerChunkZ,int width,double totalOutput) {
    public GeyserRegion {
        if(width<3||width>257||width%2==0||!Double.isFinite(totalOutput)||totalOutput<=0)
            throw new IllegalArgumentException("Geysers require an odd width and positive finite output");
    }
    public int halfWidth(){return width/2;}
    public int chunkCount(){return width*width;}
    public int blockWidth(){return width*16;}
    public double centerX(){return centerChunkX*16.0+8;}
    public double centerZ(){return centerChunkZ*16.0+8;}
    public boolean contains(int chunkX,int chunkZ){return Math.abs((long)chunkX-centerChunkX)<=halfWidth()&&Math.abs((long)chunkZ-centerChunkZ)<=halfWidth();}
    /** Discrete tent weights sum exactly to (halfWidth+1)^4 across the footprint. */
    public double outputAt(int chunkX,int chunkZ){
        if(!contains(chunkX,chunkZ))return 0;
        long axis=halfWidth()+1L;
        long x=axis-Math.abs((long)chunkX-centerChunkX),z=axis-Math.abs((long)chunkZ-centerChunkZ);
        return totalOutput*((x*z)/(double)(axis*axis*axis*axis));
    }
    /** Historical small fields retain their original deposits and reserved center. */
    public boolean legacy(){return width<=63;}
    public int coreRadiusChunks(){return legacy()?Math.max(1,halfWidth()/3):Math.min(8,Math.max(1,halfWidth()/4));}
    public int outerWidthChunks(){return legacy()?Math.max(1,halfWidth()/4):4;}
    public int innerRadiusChunks(){return coreRadiusChunks()+(halfWidth()-coreRadiusChunks()-outerWidthChunks())/2;}
    public int middleRadiusChunks(){return halfWidth()-outerWidthChunks();}
    public GeyserZone zone(int chunkX,int chunkZ){
        if(!contains(chunkX,chunkZ))return GeyserZone.OUTSIDE;
        long distance=Math.max(Math.abs((long)chunkX-centerChunkX),Math.abs((long)chunkZ-centerChunkZ));
        if(distance<=coreRadiusChunks())return GeyserZone.CORE;
        if(distance<=innerRadiusChunks())return GeyserZone.INNER;
        if(distance<=middleRadiusChunks())return GeyserZone.MIDDLE;
        return GeyserZone.OUTER;
    }
    /** No terrain shaft is reserved in new fields: world energy remains invisible data. */
    public boolean crystalZone(int chunkX,int chunkZ){return contains(chunkX,chunkZ)&&(!legacy()||zone(chunkX,chunkZ)!=GeyserZone.CORE);}
}
