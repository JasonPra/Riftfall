package org.Azkerash.riftfall.worldmana.geysers;

/** A horizontal footprint spanning every build-height layer. There is deliberately no origin Y/depth. */
public record GeyserRegion(int centerChunkX,int centerChunkZ,int width,double totalOutput,int shapeVersion) {
    /** Explicit descriptors without a shape marker retain historical square behavior. */
    public GeyserRegion(int x,int z,int width,double output){this(x,z,width,output,0);}
    public boolean circular(){return shapeVersion==1;}
    public int extentChunks(){return circular()?(int)Math.ceil((GeyserShape.radius(this)+GeyserShape.EDGE_NOISE)/16):halfWidth();}
    public GeyserRegion {
        if(shapeVersion<0||shapeVersion>1||width<3||width>257||width%2==0||!Double.isFinite(totalOutput)||totalOutput<=0)
            throw new IllegalArgumentException("Geysers require an odd width and positive finite output");
    }
    public int halfWidth(){return width/2;}
    /** Original chunk-equivalent mana budget, retained when circular terrain expands. */
    public int chunkCount(){return width*width;}
    public int blockWidth(){return width*16;}
    public double centerX(){return centerChunkX*16.0+8;}
    public double centerZ(){return centerChunkZ*16.0+8;}
    public boolean contains(int chunkX,int chunkZ){if(circular())return GeyserShape.distance(this,chunkX*16.0+8,chunkZ*16.0+8)<=0;return Math.abs((long)chunkX-centerChunkX)<=halfWidth()&&Math.abs((long)chunkZ-centerChunkZ)<=halfWidth();}
    /** Historical square tent weights or normalized radial weights; total output is unchanged. */
    public double outputAt(int chunkX,int chunkZ){
        if(!contains(chunkX,chunkZ))return 0;
        if(circular())return GeyserOutput.at(this,chunkX,chunkZ);
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
        if(circular())return GeyserShape.zone(this,chunkX*16.0+8,chunkZ*16.0+8);
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
