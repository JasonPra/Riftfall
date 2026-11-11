package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.Azkerash.riftfall.worldmana.geysers.*;

/** Chunk batches, never per-crystal tickers. Unloaded ledgers update without accessing terrain. */
public final class CrystalRefinement {
    private static final Map<ServerLevel,Map<Long,LevelChunk>> LOADED=new IdentityHashMap<>();
    public static void register(){NeoForge.EVENT_BUS.register(CrystalRefinement.class);GeyserLedger.simulation((level,region,ticks,mana,cap,regen)->RefinementStore.get(level).advance(region,ticks,mana,cap,regen));}
    private static Map<Long,CrystalTier> adults(LevelChunk chunk){
        Map<Long,CrystalTier> result=new TreeMap<>();var p=chunk.getPos();var cursor=new BlockPos.MutableBlockPos();var sections=chunk.getSections();
        for(int section=0;section<sections.length;section++){
            var data=sections[section];if(!data.maybeHas(ManaCrystalBlock::adult))continue;int base=chunk.getMinBuildHeight()+section*16;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
                BlockState state=data.getBlockState(x,y,z);if(ManaCrystalBlock.adult(state)){cursor.set(p.getMinBlockX()+x,base+y,p.getMinBlockZ()+z);result.put(cursor.asLong(),((ManaCrystalBlock)state.getBlock()).tier());}
            }
        }return result;
    }
    private static void retain(RefinementStore.Entry entry,Map<Long,CrystalTier> actual){
        entry.crystals=entry.crystals.stream().filter(c->actual.get(c.position())==c.worldTier()).toList();
    }
    private static void addNew(RefinementStore.Entry entry,Map<Long,CrystalTier> actual){
        Set<Long> known=new HashSet<>();entry.crystals.forEach(c->known.add(c.position()));var list=new ArrayList<>(entry.crystals);
        actual.forEach((pos,tier)->{if(!known.contains(pos))list.add(new RefinementBatch.Crystal(pos,tier,tier,0));});entry.crystals=List.copyOf(list);
    }
    private static void apply(ServerLevel level,LevelChunk chunk,RefinementStore.Entry entry){
        var applied=new ArrayList<RefinementBatch.Crystal>();
        for(var c:entry.crystals){
            var pos=BlockPos.of(c.position());var state=chunk.getBlockState(pos);
            if(!ManaCrystalBlock.adult(state)||((ManaCrystalBlock)state.getBlock()).tier()!=c.worldTier())continue;
            if(c.tier()!=c.worldTier()){
                var next=ManaCrystals.block(c.tier()).defaultBlockState().setValue(ManaCrystalBlock.LAYERS,8).setValue(ManaCrystalBlock.FACING,state.getValue(ManaCrystalBlock.FACING));
                if(!next.canSurvive(level,pos)||!level.setBlock(pos,next,Block.UPDATE_ALL)){applied.add(c);continue;}
            }
            applied.add(new RefinementBatch.Crystal(c.position(),c.tier(),c.tier(),c.progress()));
        }entry.crystals=List.copyOf(applied);
    }
    private static boolean applying;
    /** Called before changing an indexed block, so mined/replaced crystals cannot earn ghost upgrades. */
    public static void changing(ServerLevel level,BlockPos pos){
        if(applying)return;
        var cp=new net.minecraft.world.level.ChunkPos(pos);var field=GeyserRegions.get(level).discover(cp.x,cp.z);if(field.isEmpty())return;
        GeyserLedger.get(level).settle(level,field.orElseThrow());
        var store=RefinementStore.get(level);store.prepare(GeyserLedger.key(field.orElseThrow()));var entry=store.entries.get(cp.toLong());if(entry!=null){entry.crystals=entry.crystals.stream().filter(c->c.position()!=pos.asLong()).toList();store.setDirty();}
    }
    public static void changed(ServerLevel level,BlockPos pos){
        if(applying)return;
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)return;
        var state=chunk.getBlockState(pos);if(!ManaCrystalBlock.adult(state))return;
        var field=GeyserRegions.get(level).discover(chunk.getPos().x,chunk.getPos().z);if(field.isEmpty())return;
        GeyserLedger.get(level).settle(level,field.orElseThrow());
        var store=RefinementStore.get(level);store.prepare(GeyserLedger.key(field.orElseThrow()));var entry=store.entry(chunk.getPos().toLong());entry.region=GeyserLedger.key(field.orElseThrow());entry.exposure=1;
        var map=new HashMap<Long,CrystalTier>();map.put(pos.asLong(),((ManaCrystalBlock)state.getBlock()).tier());addNew(entry,map);store.setDirty();
    }
    private static void synchronize(ServerLevel level,LevelChunk chunk,boolean applyBlocks){
        var store=RefinementStore.get(level);var entry=store.entries.get(chunk.getPos().toLong());if(entry==null)return;
        store.prepare(entry.region);retain(entry,adults(chunk));
        var field=GeyserRegions.get(level).discover(chunk.getPos().x,chunk.getPos().z).orElseThrow();GeyserLedger.get(level).settle(level,field);
        if(applyBlocks){applying=true;try{apply(level,chunk,entry);}finally{applying=false;}}
        store.prepare(entry.region);addNew(entry,adults(chunk));store.setDirty();
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void loaded(ChunkEvent.Load event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getChunk() instanceof LevelChunk chunk))return;
        ManaCrystalBlock.refreshConnections(chunk);
        var cp=chunk.getPos();var field=GeyserRegions.get(level).discover(cp.x,cp.z);if(field.isEmpty())return;
        // Catch up existing records before adding newly discovered crystals: no retroactive refinement.
        var store=RefinementStore.get(level);store.prepare(GeyserLedger.key(field.orElseThrow()));var entry=store.entries.get(cp.toLong());if(entry!=null)retain(entry,adults(chunk));
        GeyserLedger.get(level).settle(level,field.orElseThrow());
        entry=store.entry(cp.toLong());entry.region=GeyserLedger.key(field.orElseThrow());entry.exposure=1;
        applying=true;try{apply(level,chunk,entry);}finally{applying=false;}
        store.prepare(entry.region);addNew(entry,adults(chunk));store.setDirty();LOADED.computeIfAbsent(level,k->new HashMap<>()).put(cp.toLong(),chunk);
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void unloaded(ChunkEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getChunk() instanceof LevelChunk chunk))return;var chunks=LOADED.get(level);if(chunks==null||chunks.remove(chunk.getPos().toLong())==null)return;
        synchronize(level,chunk,false);
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%20!=0)return;
        // WorldMana settles every discovered geyser, including entirely unloaded ones, first.
        var chunks=LOADED.get(level);if(chunks!=null){applying=true;try{var store=RefinementStore.get(level);for(var chunk:chunks.values()){var entry=store.entries.get(chunk.getPos().toLong());if(entry!=null){apply(level,chunk,entry);store.setDirty();}}}finally{applying=false;}}
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void stopping(ServerStoppingEvent event){
        LOADED.forEach((level,chunks)->{GeyserLedger.get(level).settleAll(level);chunks.values().forEach(chunk->synchronize(level,chunk,true));});LOADED.clear();
    }
    @SubscribeEvent public static void levelUnloaded(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)LOADED.remove(level);}
    private CrystalRefinement(){}
}
