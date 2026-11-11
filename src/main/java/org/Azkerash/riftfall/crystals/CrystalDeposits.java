package org.Azkerash.riftfall.crystals;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.*;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.worldmana.WorldMana;
import org.Azkerash.riftfall.worldmana.geysers.GeyserRegions;

/** One initial survey per cave chunk, saved independently of mana. Unloading never grants a fresh deposit. */
public final class CrystalDeposits {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,Riftfall.MODID);
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Boolean>> INITIALIZED=ATTACHMENTS.register("crystal_deposit_initialized",()->AttachmentType.builder(()->false).serialize(Codec.BOOL).build());
    private static final Map<ServerLevel,LinkedHashMap<Long,Long>> PENDING=new IdentityHashMap<>();
    public static void register(IEventBus bus){ATTACHMENTS.register(bus);NeoForge.EVENT_BUS.register(CrystalDeposits.class);}
    @SubscribeEvent public static void loaded(ChunkEvent.Load e){
        if(!(e.getLevel() instanceof ServerLevel level)||!(e.getChunk() instanceof LevelChunk chunk))return;
        var p=chunk.getPos();var field=GeyserRegions.get(level).discover(p.x,p.z);
        if(field.isPresent()&&field.orElseThrow().crystalZone(p.x,p.z))PENDING.computeIfAbsent(level,k->new LinkedHashMap<>()).putIfAbsent(p.toLong(),0L);
    }
    private static boolean present(LevelChunk chunk){for(var section:chunk.getSections())if(section.maybeHas(s->s.getBlock() instanceof ManaCrystalBlock))return true;return false;}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void tick(LevelTickEvent.Post e){
        if(!(e.getLevel() instanceof ServerLevel level))return;var queue=PENDING.get(level);if(queue==null||queue.isEmpty())return;
        long id=queue.keySet().iterator().next();long due=queue.remove(id);var cp=new ChunkPos(id);var chunk=level.getChunkSource().getChunkNow(cp.x,cp.z);if(chunk==null)return;
        if(level.getGameTime()<due){queue.put(id,due);return;}
        var field=GeyserRegions.get(level).discover(cp.x,cp.z);if(field.isEmpty())return;
        boolean exists=present(chunk);
        if(!chunk.getData(INITIALIZED)){
            if(!exists)CrystalGrowth.seedDeposit(level,chunk,field.orElseThrow(),true);
            chunk.setData(INITIALIZED,true);chunk.setUnsaved(true);
        }else if(!exists&&CrystalRules.nucleation(WorldMana.inspect(chunk).amount()))CrystalGrowth.seedDeposit(level,chunk,field.orElseThrow(),false);
        queue.put(id,level.getGameTime()+(exists||chunk.getData(INITIALIZED)?1200:20));
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload e){if(e.getLevel() instanceof ServerLevel level){var queue=PENDING.get(level);if(queue!=null)queue.remove(e.getChunk().getPos().toLong());}}
    @SubscribeEvent public static void levelUnloaded(LevelEvent.Unload e){if(e.getLevel() instanceof ServerLevel level)PENDING.remove(level);}
    @SubscribeEvent public static void stopping(ServerStoppingEvent e){PENDING.clear();}
    private CrystalDeposits(){}
}
