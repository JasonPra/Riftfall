package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Saves discovered field descriptors; no chunk references, terrain edits, entities or ticking tasks. */
public final class GeyserRegions extends SavedData {
    private final GeyserLayout layout;
    private final Map<Long,GeyserRegion> discovered=new LinkedHashMap<>();
    public GeyserRegions(long seed){layout=new GeyserLayout(seed);}
    public static GeyserRegions get(ServerLevel level){
        long seed=GeyserLayout.mix(level.getSeed()^level.dimension().location().toString().hashCode());
        var factory=new Factory<GeyserRegions>(()->{var data=new GeyserRegions(seed);data.setDirty();return data;},GeyserRegions::load,null);
        return level.getDataStorage().computeIfAbsent(factory,"riftfall_geyser_regions");
    }
    public GeyserLayout layout(){return layout;}
    public int size(){return discovered.size();}
    public Optional<GeyserRegion> candidate(int cellX,int cellZ){
        var saved=discovered.get(ChunkPos.asLong(cellX,cellZ));return saved==null?layout.cell(cellX,cellZ):Optional.of(saved);
    }
    public Collection<GeyserRegion> discovered(){return List.copyOf(discovered.values());}
    public Optional<GeyserRegion> discover(int chunkX,int chunkZ){
        long cell=ChunkPos.asLong(Math.floorDiv(chunkX,GeyserLayout.CELL_CHUNKS),Math.floorDiv(chunkZ,GeyserLayout.CELL_CHUNKS));
        var saved=discovered.get(cell);
        if(saved!=null)return saved.contains(chunkX,chunkZ)?Optional.of(saved):Optional.empty();
        var field=layout.at(chunkX,chunkZ);field.ifPresent(g->{discovered.put(cell,g);setDirty();});return field;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putInt("Version",1);tag.putLong("Seed",layout.seed());var fields=new ListTag();
        for(var g:discovered.values()){
            var entry=new CompoundTag();entry.putInt("X",g.centerChunkX());entry.putInt("Z",g.centerChunkZ());entry.putInt("Width",g.width());entry.putDouble("Output",g.totalOutput());fields.add(entry);
        }tag.put("Fields",fields);return tag;
    }
    public static GeyserRegions load(CompoundTag tag,HolderLookup.Provider provider){
        if(tag.getInt("Version")!=1)throw new IllegalArgumentException("Unsupported geyser-region data version");
        var data=new GeyserRegions(tag.getLong("Seed"));var fields=tag.getList("Fields",10);
        for(int i=0;i<fields.size();i++){
            var e=fields.getCompound(i);var g=new GeyserRegion(e.getInt("X"),e.getInt("Z"),e.getInt("Width"),e.getDouble("Output"));
            int cellX=Math.floorDiv(g.centerChunkX(),GeyserLayout.CELL_CHUNKS),cellZ=Math.floorDiv(g.centerChunkZ(),GeyserLayout.CELL_CHUNKS);
            int localX=Math.floorMod(g.centerChunkX(),GeyserLayout.CELL_CHUNKS),localZ=Math.floorMod(g.centerChunkZ(),GeyserLayout.CELL_CHUNKS);
            if(localX<g.halfWidth()||localX+g.halfWidth()>=GeyserLayout.CELL_CHUNKS||localZ<g.halfWidth()||localZ+g.halfWidth()>=GeyserLayout.CELL_CHUNKS)
                throw new IllegalArgumentException("Geyser footprint crosses its placement cell");
            if(data.discovered.put(ChunkPos.asLong(cellX,cellZ),g)!=null)throw new IllegalArgumentException("Duplicate geyser region");
        }return data;
    }
}
