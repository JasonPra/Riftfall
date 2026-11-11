package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.Azkerash.riftfall.worldmana.ChunkMana;

/** Authoritative shared reservoirs. Advancing these ledgers never loads or edits terrain. */
public final class GeyserLedger extends SavedData {
    public static final double REGENERATION_PER_CHUNK_PER_TICK=.50;
    public static final class Pool {
        public double mana;
        public long updated;
        public final double capacity, regenerationPerTick;
        Pool(GeyserRegion region,long now){capacity=capacity(region);regenerationPerTick=regeneration(region);updated=now;}
    }
    @FunctionalInterface public interface Simulation {
        double advance(ServerLevel level,GeyserRegion region,double ticks,double mana,double capacity,double regenerationPerTick);
    }
    private static Simulation simulation=(level,region,ticks,mana,cap,regen)->Math.min(cap,mana+ticks*regen);
    public static void simulation(Simulation value){simulation=Objects.requireNonNull(value);}
    private final Map<Long,Pool> pools=new HashMap<>();
    private static final Factory<GeyserLedger> FACTORY=new Factory<>(GeyserLedger::new,GeyserLedger::load,null);
    public static GeyserLedger get(ServerLevel level){return level.getDataStorage().computeIfAbsent(FACTORY,"riftfall_geyser_mana");}
    public static long key(GeyserRegion region){return ChunkPos.asLong(region.centerChunkX(),region.centerChunkZ());}
    public static double capacity(GeyserRegion region){return region.chunkCount()*ChunkMana.CAPACITY;}
    public static double regeneration(GeyserRegion region){return region.chunkCount()*REGENERATION_PER_CHUNK_PER_TICK;}
    public Pool settle(ServerLevel level,GeyserRegion region){
        var pool=pools.computeIfAbsent(key(region),k->{setDirty();return new Pool(region,level.getGameTime());});
        long now=level.getGameTime();long elapsed=Math.max(0,now-pool.updated);
        if(elapsed>0){pool.mana=simulation.advance(level,region,elapsed,pool.mana,pool.capacity,pool.regenerationPerTick);pool.updated=now;setDirty();}
        return pool;
    }
    public void settleAll(ServerLevel level){for(var region:GeyserRegions.get(level).discovered())settle(level,region);}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putInt("Version",1);var list=new ListTag();
        pools.forEach((key,p)->{var row=new CompoundTag();row.putLong("Region",key);row.putDouble("Mana",p.mana);row.putLong("Updated",p.updated);row.putDouble("Capacity",p.capacity);row.putDouble("RegenerationPerTick",p.regenerationPerTick);list.add(row);});
        tag.put("Pools",list);return tag;
    }
    public static GeyserLedger load(CompoundTag tag,HolderLookup.Provider provider){
        if(tag.getInt("Version")!=1)throw new IllegalArgumentException("Unsupported geyser mana ledger");
        var data=new GeyserLedger();var list=tag.getList("Pools",10);
        for(int i=0;i<list.size();i++){
            var row=list.getCompound(i);long key=row.getLong("Region");double cap=row.getDouble("Capacity"),regen=row.getDouble("RegenerationPerTick"),mana=row.getDouble("Mana");
            int width=(int)Math.round(Math.sqrt(cap/ChunkMana.CAPACITY));
            var region=new GeyserRegion(ChunkPos.getX(key),ChunkPos.getZ(key),width,1);
            var pool=new Pool(region,row.getLong("Updated"));
            if(!Double.isFinite(mana)||mana<0||mana>pool.capacity||cap!=pool.capacity||regen!=pool.regenerationPerTick)throw new IllegalArgumentException("Invalid geyser pool");
            pool.mana=mana;if(data.pools.put(key,pool)!=null)throw new IllegalArgumentException("Duplicate geyser pool");
        }return data;
    }
}
