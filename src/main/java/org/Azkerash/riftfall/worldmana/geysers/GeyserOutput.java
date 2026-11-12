package org.Azkerash.riftfall.worldmana.geysers;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded descriptor-only normalization; total output remains exact after the shape changes. */
final class GeyserOutput {
    private static final Map<GeyserRegion,Double> SUMS = new LinkedHashMap<>(128,.75F,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<GeyserRegion,Double> eldest) { return size()>128; }
    };
    private static double weight(GeyserRegion g,int x,int z) {
        if(!g.contains(x,z))return 0;
        double distance=Math.hypot((x-g.centerChunkX())*16.0,(z-g.centerChunkZ())*16.0);
        return Math.pow(Math.max(.001,1-distance/(GeyserShape.radius(g)+GeyserShape.EDGE_NOISE)),2);
    }
    static double at(GeyserRegion g,int x,int z) {
        double sum;
        synchronized(SUMS) {
            sum=SUMS.computeIfAbsent(g,key->{
                double total=0;int e=g.extentChunks();
                for(int dx=-e;dx<=e;dx++)for(int dz=-e;dz<=e;dz++)total+=weight(g,g.centerChunkX()+dx,g.centerChunkZ()+dz);
                return total;
            });
        }
        return g.totalOutput()*(weight(g,x,z)/sum);
    }
    private GeyserOutput() {}
}
