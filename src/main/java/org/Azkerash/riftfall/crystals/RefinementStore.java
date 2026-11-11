package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.Azkerash.riftfall.worldmana.geysers.*;

/** Crystal tallies only. The separate GeyserLedger owns the sole authoritative mana balance. */
public final class RefinementStore extends SavedData {
    private static final Factory<RefinementStore> FACTORY=new Factory<>(RefinementStore::new,RefinementStore::load,null);
    static final class Entry {
        long region; double exposure;
        List<RefinementBatch.Crystal> crystals=List.of();
    }
    final Map<Long,Entry> entries=new LinkedHashMap<>();
    final Map<Long,Set<Long>> rounds=new HashMap<>();
    private static final class Clock {double owed,deadline;final RefinementBatch.Settings settings;Clock(double deadline,RefinementBatch.Settings settings){this.deadline=deadline;this.settings=settings;}}
    private final Map<Long,Clock> clocks=new HashMap<>();
    /** Materialize lazy parallel progress before changing a region's crystal membership. */
    void prepare(long region){
        var clock=clocks.remove(region);if(clock==null||clock.owed==0)return;
        var speeds=RefinementNeighbors.speeds(crystals(region),clock.settings.boosts());
        for(var entry:entries.values())if(entry.region==region)entry.crystals=entry.crystals.stream().map(c->new RefinementBatch.Crystal(c.position(),c.worldTier(),c.tier(),c.tier()==CrystalTier.WHITE?0:Math.min(clock.settings.ticks(c.tier()),c.progress()+clock.owed*entry.exposure*speeds.get(c.position())),c.contacts())).toList();
        setDirty();
    }
    private List<RefinementBatch.Crystal> crystals(long region){return entries.values().stream().filter(e->e.region==region).flatMap(e->e.crystals.stream()).toList();}
    public static RefinementStore get(ServerLevel level){return level.getDataStorage().computeIfAbsent(FACTORY,"riftfall_crystal_refinement");}
    Entry entry(long id){return entries.computeIfAbsent(id,k->{setDirty();return new Entry();});}
    double advance(GeyserRegion region,double elapsed,double mana,double capacity,double regenerationPerTick){
        long key=GeyserLedger.key(region);
        var settings=CrystalRules.refinement();
        var clock=clocks.get(key);
        if(clock!=null&&!clock.settings.equals(settings)){prepare(key);clock=null;}
        if(clock==null){
            double deadline=Double.POSITIVE_INFINITY;
            var speeds=RefinementNeighbors.speeds(crystals(key),settings.boosts());
            for(var entry:entries.values())if(entry.region==key)for(var crystal:entry.crystals)if(crystal.tier()!=CrystalTier.WHITE)deadline=Math.min(deadline,Math.max(0,settings.ticks(crystal.tier())-crystal.progress())/(entry.exposure*speeds.get(crystal.position())));
            clock=new Clock(deadline,settings);clocks.put(key,clock);
        }
        double wait=mana>=settings.manaCost()?0:regenerationPerTick>0?(settings.manaCost()-mana)/regenerationPerTick:Double.POSITIVE_INFINITY;
        double active=Math.max(0,elapsed-wait);
        if(clock.owed+active<clock.deadline){if(Double.isFinite(clock.deadline))clock.owed+=active;setDirty();return Math.min(capacity,mana+elapsed*regenerationPerTick);}
        prepare(key);
        var chunks=entries.entrySet().stream().filter(e->e.getValue().region==key&&!e.getValue().crystals.isEmpty())
            .sorted(Comparator.<Map.Entry<Long,Entry>>comparingLong(e->{long dx=(long)ChunkPos.getX(e.getKey())-region.centerChunkX(),dz=(long)ChunkPos.getZ(e.getKey())-region.centerChunkZ();return dx*dx+dz*dz;}).thenComparingLong(Map.Entry::getKey))
            .map(e->new RefinementBatch.Chunk(e.getKey(),e.getValue().exposure,e.getValue().crystals)).toList();
        if(chunks.isEmpty())return Math.min(capacity,mana+elapsed*regenerationPerTick);
        var result=RefinementBatch.advance(chunks,elapsed,mana,capacity,regenerationPerTick,CrystalRules.refinement(),rounds.getOrDefault(key,Set.of()));
        result.chunks().forEach(c->entries.get(c.id()).crystals=c.crystals());rounds.put(key,result.served());setDirty();return result.mana();
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        for(long key:new ArrayList<>(clocks.keySet()))prepare(key);
        tag.putInt("Version",2);var chunks=new ListTag();
        entries.forEach((id,e)->{
            var row=new CompoundTag();row.putLong("Chunk",id);row.putLong("Region",e.region);row.putDouble("Exposure",e.exposure);
            var crystals=new ListTag();for(var crystal:e.crystals){var c=new CompoundTag();c.putLong("Position",crystal.position());c.putString("WorldTier",crystal.worldTier().name());c.putString("Tier",crystal.tier().name());c.putDouble("Progress",crystal.progress());c.putInt("Contacts",crystal.contacts());crystals.add(c);}row.put("Crystals",crystals);chunks.add(row);
        });tag.put("Chunks",chunks);
        var savedRounds=new ListTag();rounds.forEach((id,served)->{var row=new CompoundTag();row.putLong("Region",id);row.putLongArray("Served",served.stream().mapToLong(Long::longValue).toArray());savedRounds.add(row);});tag.put("Rounds",savedRounds);return tag;
    }
    public static RefinementStore load(CompoundTag tag,HolderLookup.Provider provider){
        if(tag.getInt("Version")!=2)throw new IllegalArgumentException("Unsupported refinement data version");
        var store=new RefinementStore();var chunks=tag.getList("Chunks",10);
        for(int i=0;i<chunks.size();i++){
            var row=chunks.getCompound(i);long id=row.getLong("Chunk");var e=new Entry();e.region=row.getLong("Region");e.exposure=row.getDouble("Exposure");
            if(!Double.isFinite(e.exposure)||e.exposure<=0)throw new IllegalArgumentException("Invalid refinement ledger");
            var list=row.getList("Crystals",10);var progress=new ArrayList<RefinementBatch.Crystal>();Set<Long> seen=new HashSet<>();
            for(int j=0;j<list.size();j++){var c=list.getCompound(j);long pos=c.getLong("Position");if(!seen.add(pos))throw new IllegalArgumentException("Duplicate refinement position");progress.add(new RefinementBatch.Crystal(pos,CrystalTier.valueOf(c.getString("WorldTier")),CrystalTier.valueOf(c.getString("Tier")),c.getDouble("Progress"),c.getInt("Contacts")));}
            e.crystals=List.copyOf(progress);if(store.entries.put(id,e)!=null)throw new IllegalArgumentException("Duplicate refinement chunk");
        }
        var rounds=tag.getList("Rounds",10);for(int i=0;i<rounds.size();i++){var row=rounds.getCompound(i);var served=new HashSet<Long>();for(long id:row.getLongArray("Served"))served.add(id);store.rounds.put(row.getLong("Region"),Set.copyOf(served));}
        return store;
    }
}
