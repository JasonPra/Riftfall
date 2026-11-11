package org.Azkerash.riftfall.worldmana;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
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

/** Server-owned loaded-chunk reservoirs and geyser flow. Never forces neighboring chunks to load. */
public final class WorldMana {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,Riftfall.MODID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<ChunkMana>> MANA=ATTACHMENTS.register("ambient_mana",()->AttachmentType.builder(ChunkMana::new).serialize(ChunkMana.CODEC).build());
    private static final Map<ServerLevel,Map<Long,Tracked>> LOADED=new IdentityHashMap<>();
    private static final class Tracked {
        final LevelChunk chunk;
        final boolean geyser;
        long settled,lastFlow;
        Tracked(LevelChunk chunk,long tick,boolean geyser){this.chunk=chunk;settled=lastFlow=tick;this.geyser=geyser;}
        double base(){return geyser?LeyLineNetwork.GEYSER_BASE_REGENERATION:ChunkMana.BASE_REGENERATION;}
        void settle(long now){
            long elapsed=Math.max(0,now-settled);settled=now;
            if(chunk.getData(MANA).regenerate(elapsed,base())>0)chunk.setUnsaved(true);
        }
    }
    public static void register(IEventBus bus){ATTACHMENTS.register(bus);NeoForge.EVENT_BUS.register(WorldMana.class);NeoForge.EVENT_BUS.register(WorldManaCommands.class);}
    private static ServerLevel server(LevelChunk chunk){
        if(!(chunk.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread())throw new IllegalStateException("Chunk mana must be accessed on the server thread");
        return level;
    }
    private static Tracked tracked(LevelChunk chunk){var level=server(chunk);return LOADED.getOrDefault(level,Map.of()).get(chunk.getPos().toLong());}
    private static ChunkMana settled(LevelChunk chunk){var tracked=tracked(chunk);if(tracked!=null)tracked.settle(chunk.getLevel().getGameTime());return chunk.getData(MANA);}
    public static ChunkMana.Snapshot inspect(LevelChunk chunk){
        var mana=settled(chunk);var tracked=tracked(chunk);
        return mana.snapshot(tracked==null?ChunkMana.BASE_REGENERATION:tracked.base());
    }
    public static double add(LevelChunk chunk,double amount){double accepted=settled(chunk).add(amount);if(accepted>0)chunk.setUnsaved(true);return accepted;}
    public static double extract(LevelChunk chunk,double amount){double taken=settled(chunk).extract(amount);if(taken>0)chunk.setUnsaved(true);return taken;}
    public static boolean tryExtract(LevelChunk chunk,double amount){boolean taken=settled(chunk).tryExtract(amount);if(taken&&amount>0)chunk.setUnsaved(true);return taken;}
    /** Crystal block lifecycle will call this; ores and metals never affect generation. */
    public static void setMatureCrystalCount(LevelChunk chunk,int count){
        if(settled(chunk).setMatureCrystals(count)){
            chunk.setUnsaved(true);var level=server(chunk);var pos=chunk.getPos();
            LeyLineNetwork.get(level).updateCrystals(pos.x,pos.z,count);
        }
    }
    public static long placementSeed(ServerLevel level){return LeyLinePlacement.mix(level.getSeed()^level.dimension().location().toString().hashCode());}
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getChunk() instanceof LevelChunk chunk))return;
        boolean existed=chunk.hasData(MANA);chunk.getData(MANA);if(!existed)chunk.setUnsaved(true);
        var pos=chunk.getPos();var candidate=LeyLinePlacement.at(placementSeed(level),pos.x,pos.z);boolean geyser=candidate.isPresent();
        if(geyser){
            var network=LeyLineNetwork.get(level);
            network.discover(pos.x,pos.z,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,8,8)+1,candidate.orElseThrow().priority());
            network.updateCrystals(pos.x,pos.z,chunk.getData(MANA).matureCrystals());
        }
        LOADED.computeIfAbsent(level,k->new HashMap<>()).putIfAbsent(pos.toLong(),new Tracked(chunk,level.getGameTime(),geyser));
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var chunks=LOADED.get(level);if(chunks==null)return;
        var tracked=chunks.remove(event.getChunk().getPos().toLong());if(tracked!=null)tracked.settle(level.getGameTime());
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%20!=0)return;
        var chunks=LOADED.get(level);if(chunks==null)return;
        long now=level.getGameTime();for(var tracked:chunks.values())tracked.settle(now);
        transfer(level,chunks,now);
    }
    private static void transfer(ServerLevel level,Map<Long,Tracked> chunks,long now){
        List<LeyLineFlow.Reservoir> reservoirs=new ArrayList<>();List<LeyLineFlow.Connection> connections=new ArrayList<>();
        LeyLineNetwork network=null;
        for(var tracked:chunks.values()){
            if(!tracked.geyser)continue;
            if(network==null)network=LeyLineNetwork.get(level);
            var pos=tracked.chunk.getPos();var node=network.node(pos.x,pos.z).orElseThrow();
            var mana=tracked.chunk.getData(MANA);
            reservoirs.add(new LeyLineFlow.Reservoir(node.id(),mana.amount(),ChunkMana.CAPACITY,node.strength(),Math.max(0,now-tracked.lastFlow)/20.0));
            tracked.lastFlow=now;
            if(node.parent()!=null)connections.add(new LeyLineFlow.Connection(node.id(),node.parent()));
        }
        for(var transfer:LeyLineFlow.plan(reservoirs,connections)){
            var source=chunks.get(transfer.source()).chunk;var destination=chunks.get(transfer.destination()).chunk;
            double taken=source.getData(MANA).extract(transfer.amount());
            double accepted=destination.getData(MANA).add(taken);
            if(accepted<taken)source.getData(MANA).add(taken-accepted);
            if(accepted>0){source.setUnsaved(true);destination.setUnsaved(true);}
        }
    }
    @SubscribeEvent public static void levelUnloaded(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)LOADED.remove(level);}
    @SubscribeEvent public static void stopping(ServerStoppingEvent event){
        LOADED.forEach((level,chunks)->chunks.values().forEach(tracked->tracked.settle(level.getGameTime())));LOADED.clear();
    }
    private WorldMana(){}
}
