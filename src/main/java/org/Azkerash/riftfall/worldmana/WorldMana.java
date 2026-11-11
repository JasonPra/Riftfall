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

/** Server-owned loaded-chunk bookkeeping. No forced loads or offline catch-up. */
public final class WorldMana {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,Riftfall.MODID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<ChunkMana>> MANA=ATTACHMENTS.register("ambient_mana",()->AttachmentType.builder(ChunkMana::new).serialize(ChunkMana.CODEC).build());
    private static final Map<ServerLevel,Map<Long,Tracked>> LOADED=new IdentityHashMap<>();
    private static final class Tracked {
        final LevelChunk chunk;long settled;
        Tracked(LevelChunk chunk,long tick){this.chunk=chunk;settled=tick;}
        void settle(long now){long elapsed=Math.max(0,now-settled);settled=now;if(chunk.getData(MANA).regenerate(elapsed)>0)chunk.setUnsaved(true);}
    }
    public static void register(IEventBus bus){ATTACHMENTS.register(bus);NeoForge.EVENT_BUS.register(WorldMana.class);NeoForge.EVENT_BUS.register(WorldManaCommands.class);}
    private static ServerLevel server(LevelChunk chunk){if(!(chunk.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread())throw new IllegalStateException("Chunk mana must be accessed on the server thread");return level;}
    private static ChunkMana settled(LevelChunk chunk){var level=server(chunk);var tracked=LOADED.getOrDefault(level,Map.of()).get(chunk.getPos().toLong());if(tracked!=null)tracked.settle(level.getGameTime());return chunk.getData(MANA);}
    public static ChunkMana.Snapshot inspect(LevelChunk chunk){return settled(chunk).snapshot();}
    public static double add(LevelChunk chunk,double amount){double accepted=settled(chunk).add(amount);if(accepted>0)chunk.setUnsaved(true);return accepted;}
    public static double extract(LevelChunk chunk,double amount){double taken=settled(chunk).extract(amount);if(taken>0)chunk.setUnsaved(true);return taken;}
    /** Future crystal blocks must update this count on maturity, removal and cross-chunk movement. */
    public static void setMatureCrystalCount(LevelChunk chunk,int count){if(settled(chunk).setMatureCrystals(count))chunk.setUnsaved(true);}
    public static long placementSeed(ServerLevel level){return LeyLinePlacement.mix(level.getSeed()^level.dimension().location().toString().hashCode());}
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getChunk() instanceof LevelChunk chunk))return;
        boolean existed=chunk.hasData(MANA);chunk.getData(MANA);if(!existed)chunk.setUnsaved(true);
        LOADED.computeIfAbsent(level,k->new HashMap<>()).putIfAbsent(chunk.getPos().toLong(),new Tracked(chunk,level.getGameTime()));
        var pos=chunk.getPos();LeyLinePlacement.at(placementSeed(level),pos.x,pos.z).ifPresent(candidate->{
            int y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,8,8)+1;
            LeyLineNetwork.get(level).discover(pos.x,pos.z,y,candidate.priority());
        });
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var chunks=LOADED.get(level);if(chunks==null)return;
        var tracked=chunks.remove(event.getChunk().getPos().toLong());if(tracked!=null)tracked.settle(level.getGameTime());
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%20!=0)return;
        var chunks=LOADED.get(level);if(chunks!=null)for(var tracked:chunks.values())tracked.settle(level.getGameTime());
    }
    @SubscribeEvent public static void levelUnloaded(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)LOADED.remove(level);}
    @SubscribeEvent public static void stopping(ServerStoppingEvent event){
        LOADED.forEach((level,chunks)->chunks.values().forEach(tracked->tracked.settle(level.getGameTime())));LOADED.clear();
    }
    private WorldMana(){}
}
