package org.Azkerash.riftfall.worldmana.geysers;

/** Seeded, exploration-order-independent selection; no saved or runtime spawning rolls. */
public final class GoblinCampLayout {
    public static final int CELL_CHUNKS = 8;
    public static boolean eligible(long seed, GeyserRegion field) {
        return Math.floorMod(GeyserLayout.mix(seed ^ 0x474F424C494E4341L
            ^ field.centerChunkX() * 341873128712L ^ field.centerChunkZ() * 132897987541L), 3) == 0;
    }
    public static boolean candidate(long seed, int cx, int cz) {
        int cellX = Math.floorDiv(cx, CELL_CHUNKS), cellZ = Math.floorDiv(cz, CELL_CHUNKS);
        long h = GeyserLayout.mix(seed ^ 0x43414D5053495445L ^ cellX * 341873128712L ^ cellZ * 132897987541L);
        if ((h & 1) != 0) return false;
        return cx == cellX * CELL_CHUNKS + 2 + (int)((h >>> 8) & 3)
            && cz == cellZ * CELL_CHUNKS + 2 + (int)((h >>> 16) & 3);
    }
    private GoblinCampLayout() {}
}
