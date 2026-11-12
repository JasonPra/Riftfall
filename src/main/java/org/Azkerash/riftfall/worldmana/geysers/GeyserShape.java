package org.Azkerash.riftfall.worldmana.geysers;

/** Continuous world-space circles with gently frayed boundaries; never quantized to chunks. */
public final class GeyserShape {
    public static final double EDGE_NOISE = 32;
    private static double squareArea(int radiusChunks) { return Math.pow((radiusChunks * 2 + 1) * 16.0, 2); }
    /** Preserve every former zone's area even at the worst permitted inward/outward noise. */
    public static double coreRadius(GeyserRegion g) { return g.circular() ? 168 : (g.coreRadiusChunks() + .5) * 16; }
    public static double innerRadius(GeyserRegion g) {
        if(!g.circular())return (g.innerRadiusChunks()+.5)*16;
        double area=squareArea(g.innerRadiusChunks())-squareArea(g.coreRadiusChunks());
        return Math.sqrt(Math.pow(coreRadius(g)+EDGE_NOISE*.35,2)+area/Math.PI)+EDGE_NOISE*.7;
    }
    public static double middleRadius(GeyserRegion g) {
        if(!g.circular())return g.blockWidth()/2.0-g.outerWidthChunks()*16;
        double area=squareArea(g.middleRadiusChunks())-squareArea(g.innerRadiusChunks());
        return Math.sqrt(Math.pow(innerRadius(g)+EDGE_NOISE*.7,2)+area/Math.PI)+EDGE_NOISE;
    }
    public static double radius(GeyserRegion g) {
        if(!g.circular())return g.blockWidth()/2.0;
        double area=squareArea(g.halfWidth())-squareArea(g.middleRadiusChunks());
        return Math.sqrt(Math.pow(middleRadius(g)+EDGE_NOISE,2)+area/Math.PI)+EDGE_NOISE;
    }
    private static double smooth(double t) { return t * t * (3 - 2 * t); }
    private static double value(long seed, long x, long z) {
        return (GeyserLayout.mix(seed ^ x * 341873128712L ^ z * 132897987541L) >>> 11) * 0x1.0p-52 - 1;
    }
    private static double noise(long seed, double x, double z, double scale) {
        x /= scale; z /= scale;
        long ix = (long)Math.floor(x), iz = (long)Math.floor(z);
        double tx = smooth(x - ix), tz = smooth(z - iz);
        double a = value(seed, ix, iz), b = value(seed, ix + 1, iz);
        double c = value(seed, ix, iz + 1), d = value(seed, ix + 1, iz + 1);
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }
    public static double fray(GeyserRegion g, double x, double z) {
        long seed = GeyserLayout.mix(g.centerChunkX() * 731L ^ g.centerChunkZ() * 997L);
        return 20 * noise(seed, x, z, 144) + 8 * noise(seed + 1, x, z, 48) + 4 * noise(seed + 2, x, z, 16);
    }
    public static double distance(GeyserRegion g, double x, double z) {
        double dx = Math.abs(x - g.centerX()), dz = Math.abs(z - g.centerZ());
        return g.circular() ? Math.hypot(dx, dz) - radius(g) - fray(g, x, z) : Math.max(dx, dz) - radius(g);
    }
    public static GeyserZone zone(GeyserRegion g, double x, double z) {
        if (!g.circular()) return g.zone((int)Math.floor(x) >> 4, (int)Math.floor(z) >> 4);
        double d = Math.hypot(x - g.centerX(), z - g.centerZ()), edge = fray(g, x, z);
        if (d > radius(g) + edge) return GeyserZone.OUTSIDE;
        if (d <= coreRadius(g) + edge * .35) return GeyserZone.CORE;
        if (d <= innerRadius(g) + edge * .7) return GeyserZone.INNER;
        if (d <= middleRadius(g) + edge) return GeyserZone.MIDDLE;
        return GeyserZone.OUTER;
    }
    private GeyserShape() {}
}
