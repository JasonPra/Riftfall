package org.Azkerash.riftfall.worldmana;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.*;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.worldmana.geysers.*;

/** Ordinary loaded-chunk reservoirs and shared full-height geyser pools. Never forces chunk loads. */
public final class WorldMana {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,Riftfall.MODID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<ChunkMana>> MANA=ATTACHMENTS.register("ambient_mana",()->AttachmentType.builder(ChunkMana::new).serialize(ChunkMana.CODEC).build());
    private static final Map<ServerLevel,Map<Long,Tracked>> LOADED=new IdentityHashMap<>();
    private static final class Tracked {
        final LevelChunk chunk;
        long settled;
        final boolean geyser;
        Tracked(LevelChunk chunk,long tick,boolean geyser){this.chunk=chunk;settled=tick;this.geyser=geyser;}
        void settle(long now){
            long elapsed=Math.max(0,now-settled);settled=now;
            if(!geyser&&chunk.getData(MANA).regenerate(elapsed)>0)chunk.setUnsaved(true);
        }
    }
    public static void register(IEventBus bus){ATTACHMENTS.register(bus);NeoForge.EVENT_BUS.register(WorldMana.class);NeoForge.EVENT_BUS.register(WorldManaCommands.class);NeoForge.EVENT_BUS.register(org.Azkerash.riftfall.worldmana.geysers.GeyserCommands.class);}
    private static ServerLevel server(LevelChunk chunk){
        if(!(chunk.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread())throw new IllegalStateException("Chunk mana must be accessed on the server thread");
        return level;
    }
    private static ChunkMana settled(LevelChunk chunk){
        var level=server(chunk);var tracked=LOADED.getOrDefault(level,Map.of()).get(chunk.getPos().toLong());
        if(tracked!=null)tracked.settle(level.getGameTime());return chunk.getData(MANA);
    }
    private static Optional<GeyserRegion> field(LevelChunk chunk){var level=server(chunk);return GeyserRegions.get(level).discover(chunk.getPos().x,chunk.getPos().z);}
    public static ChunkMana.Snapshot inspect(LevelChunk chunk){
        var field=field(chunk);if(field.isPresent()){var p=GeyserLedger.get(server(chunk)).settle(server(chunk),field.orElseThrow());return new ChunkMana.Snapshot(p.mana,p.capacity,p.regenerationPerTick*20);}
        return settled(chunk).snapshot();
    }
    private static void quantity(double amount){if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid mana amount");}
    public static double add(LevelChunk chunk,double amount){
        quantity(amount);var field=field(chunk);if(field.isPresent()){var ledger=GeyserLedger.get(server(chunk));var p=ledger.settle(server(chunk),field.orElseThrow());double accepted=Math.min(amount,p.capacity-p.mana);p.mana+=accepted;if(accepted>0)ledger.setDirty();return accepted;}
        double accepted=settled(chunk).add(amount);if(accepted>0)chunk.setUnsaved(true);return accepted;
    }
    public static double extract(LevelChunk chunk,double amount){
        quantity(amount);var field=field(chunk);if(field.isPresent()){var ledger=GeyserLedger.get(server(chunk));var p=ledger.settle(server(chunk),field.orElseThrow());double taken=Math.min(amount,p.mana);p.mana-=taken;if(taken>0)ledger.setDirty();return taken;}
        double taken=settled(chunk).extract(amount);if(taken>0)chunk.setUnsaved(true);return taken;
    }
    public static boolean tryExtract(LevelChunk chunk,double amount){
        quantity(amount);var field=field(chunk);if(field.isPresent()){var ledger=GeyserLedger.get(server(chunk));var p=ledger.settle(server(chunk),field.orElseThrow());if(p.mana<amount)return false;p.mana-=amount;if(amount>0)ledger.setDirty();return true;}
        boolean taken=settled(chunk).tryExtract(amount);if(taken&&amount>0)chunk.setUnsaved(true);return taken;
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getChunk() instanceof LevelChunk chunk))return;
        boolean existed=chunk.hasData(MANA);chunk.getData(MANA);if(!existed)chunk.setUnsaved(true);
        var field=GeyserRegions.get(level).discover(chunk.getPos().x,chunk.getPos().z);
        if(field.isPresent()){
            var ledger=GeyserLedger.get(level);var pool=ledger.settle(level,field.orElseThrow());
            // Migrate any saved independent balance exactly once; geyser attachments remain empty.
            double legacy=chunk.getData(MANA).extract(Double.MAX_VALUE);
            if(legacy>0){pool.mana=Math.min(pool.capacity,pool.mana+legacy);ledger.setDirty();chunk.setUnsaved(true);}
        }
        LOADED.computeIfAbsent(level,k->new HashMap<>()).putIfAbsent(chunk.getPos().toLong(),new Tracked(chunk,level.getGameTime(),field.isPresent()));
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var chunks=LOADED.get(level);if(chunks==null)return;
        var tracked=chunks.remove(event.getChunk().getPos().toLong());if(tracked!=null)tracked.settle(level.getGameTime());
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%20!=0)return;
        GeyserLedger.get(level).settleAll(level);
        var chunks=LOADED.get(level);if(chunks!=null)for(var tracked:chunks.values())tracked.settle(level.getGameTime());
    }
    @SubscribeEvent public static void levelUnloaded(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)LOADED.remove(level);}
    @SubscribeEvent public static void stopping(ServerStoppingEvent event){
        LOADED.forEach((level,chunks)->chunks.values().forEach(tracked->tracked.settle(level.getGameTime())));LOADED.clear();
    }
    private WorldMana(){}
}
