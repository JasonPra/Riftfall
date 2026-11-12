package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Saved field descriptors with separate legacy/new spatial indexes; no terrain loads. */
public final class GeyserRegions extends SavedData {
    private static final int LEGACY_CELL=128;
    private final GeyserLayout layout;
    private final Map<Long,GeyserRegion> discovered=new LinkedHashMap<>();
    private final Map<Long,GeyserRegion> legacyCells=new HashMap<>(),modernCells=new HashMap<>();
    public GeyserRegions(long seed){layout=new GeyserLayout(seed);}
    public static GeyserRegions get(ServerLevel level){
        long seed=GeyserLayout.mix(level.getSeed()^level.dimension().location().toString().hashCode());
        var factory=new Factory<GeyserRegions>(()->{var data=new GeyserRegions(seed);data.setDirty();return data;},GeyserRegions::load,null);
        synchronized(level.getDataStorage()){return level.getDataStorage().computeIfAbsent(factory,"riftfall_geyser_regions");}
    }
    public GeyserLayout layout(){return layout;}
    public synchronized boolean legacyAt(int x,int z){var g=legacyCells.get(cell(x,z,LEGACY_CELL));return g!=null&&g.contains(x,z);}
    public synchronized int size(){return discovered.size();}
    private static long cell(int x,int z,int size){return ChunkPos.asLong(Math.floorDiv(x,size),Math.floorDiv(z,size));}
    public synchronized Optional<GeyserRegion> candidate(int cellX,int cellZ){
        var saved=modernCells.get(ChunkPos.asLong(cellX,cellZ));if(saved!=null)return Optional.of(saved);
        return layout.cell(cellX,cellZ).filter(g->!overlapsLegacy(g));
    }
    private boolean overlapsLegacy(GeyserRegion region){
        int radius=region.extentChunks();
        for(int x=Math.floorDiv(region.centerChunkX()-radius,LEGACY_CELL);x<=Math.floorDiv(region.centerChunkX()+radius,LEGACY_CELL);x++)
            for(int z=Math.floorDiv(region.centerChunkZ()-radius,LEGACY_CELL);z<=Math.floorDiv(region.centerChunkZ()+radius,LEGACY_CELL);z++){
                var old=legacyCells.get(ChunkPos.asLong(x,z));
                if(old!=null&&Math.abs((long)old.centerChunkX()-region.centerChunkX())<=old.halfWidth()+radius&&Math.abs((long)old.centerChunkZ()-region.centerChunkZ())<=old.halfWidth()+radius)return true;
            }
        return false;
    }
    public synchronized Collection<GeyserRegion> discovered(){return List.copyOf(discovered.values());}
    public synchronized Optional<GeyserRegion> discover(int chunkX,int chunkZ){
        var old=legacyCells.get(cell(chunkX,chunkZ,LEGACY_CELL));if(old!=null&&old.contains(chunkX,chunkZ))return Optional.of(old);
        int x=Math.floorDiv(chunkX,GeyserLayout.CELL_CHUNKS),z=Math.floorDiv(chunkZ,GeyserLayout.CELL_CHUNKS);
        var field=candidate(x,z).filter(g->g.contains(chunkX,chunkZ));
        field.ifPresent(g->{if(!modernCells.containsKey(ChunkPos.asLong(x,z))){modernCells.put(ChunkPos.asLong(x,z),g);discovered.put(GeyserLedger.key(g),g);setDirty();}});
        return field;
    }
    private void addSaved(GeyserRegion region,boolean legacy){
        if(legacy!=region.legacy())throw new IllegalArgumentException("Invalid field generation marker");
        int size=legacy?LEGACY_CELL:GeyserLayout.CELL_CHUNKS;
        int localX=Math.floorMod(region.centerChunkX(),size),localZ=Math.floorMod(region.centerChunkZ(),size);
        if(localX<region.extentChunks()||localX+region.extentChunks()>=size||localZ<region.extentChunks()||localZ+region.extentChunks()>=size)throw new IllegalArgumentException("Geyser footprint crosses its placement cell");
        var index=legacy?legacyCells:modernCells;
        if(index.put(cell(region.centerChunkX(),region.centerChunkZ(),size),region)!=null||discovered.put(GeyserLedger.key(region),region)!=null)throw new IllegalArgumentException("Duplicate geyser region");
    }
    @Override public synchronized CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putInt("Version",3);tag.putLong("Seed",layout.seed());var fields=new ListTag();
        for(var g:discovered.values()){
            var entry=new CompoundTag();entry.putInt("X",g.centerChunkX());entry.putInt("Z",g.centerChunkZ());entry.putInt("Width",g.width());entry.putDouble("Output",g.totalOutput());entry.putBoolean("Legacy",g.legacy());entry.putInt("Shape",g.shapeVersion());fields.add(entry);
        }tag.put("Fields",fields);return tag;
    }
    public static GeyserRegions load(CompoundTag tag,HolderLookup.Provider provider){
        int version=tag.getInt("Version");if(version!=1&&version!=2&&version!=3)throw new IllegalArgumentException("Unsupported geyser-region data version");
        var data=new GeyserRegions(tag.getLong("Seed"));var fields=tag.getList("Fields",10);
        for(int i=0;i<fields.size();i++){
            var entry=fields.getCompound(i);var region=new GeyserRegion(entry.getInt("X"),entry.getInt("Z"),entry.getInt("Width"),entry.getDouble("Output"),version>=3?entry.getInt("Shape"):0);
            data.addSaved(region,version==1||entry.getBoolean("Legacy"));
        }
        for(var modern:data.modernCells.values())if(data.overlapsLegacy(modern))throw new IllegalArgumentException("Modern geyser overlaps a preserved legacy field");
        if(version==1)data.setDirty();return data;
    }
}
