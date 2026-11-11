package org.Azkerash.riftfall.worldmana.geysers;

/** A horizontal footprint spanning every build-height layer. There is deliberately no origin Y/depth. */
public record GeyserRegion(int centerChunkX,int centerChunkZ,int width,double totalOutput) {
    public GeyserRegion {
        if(width<3||width>63||width%2==0||!Double.isFinite(totalOutput)||totalOutput<=0)
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
    /** A stable surrounding zone for future crystals. The central stream is reserved. */
    public boolean crystalZone(int chunkX,int chunkZ){
        if(!contains(chunkX,chunkZ))return false;
        long distance=Math.max(Math.abs((long)chunkX-centerChunkX),Math.abs((long)chunkZ-centerChunkZ));
        return distance>Math.max(1,halfWidth()/3);
    }
}
