package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Universal physical drift for river water, independent of water block flow levels. */
@EventBusSubscriber(modid="riftfall")
public final class RiverCurrents {
    private record Cached(long until,RiverCurrentField.Flow flow){}
    private static final Map<ServerLevel,LinkedHashMap<Long,Cached>> CACHE=new WeakHashMap<>();
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        var entity=event.getEntity();if(!(entity.level() instanceof ServerLevel level))return;
        if(!entity.isInWater()&&!(entity instanceof net.minecraft.world.entity.vehicle.Boat&&level.getFluidState(entity.blockPosition().below()).is(FluidTags.WATER)))return;
        int x=entity.getBlockX(),z=entity.getBlockZ();var pos=entity.blockPosition();
        var biome=level.getBiome(pos);var field=GeyserBiomes.field(level,x>>4,z>>4);
        boolean zoneBiome=biome.unwrapKey().map(k->k.location().getNamespace().equals("riftfall")&&k.location().getPath().startsWith("geyser_")).orElse(false);
        boolean geyserRiver=zoneBiome&&field!=null&&GeyserTerrain.riverDistance(field,x,z)<24;
        if(!biome.is(BiomeTags.IS_RIVER)&&!geyserRiver)return;
        int surface=waterHeight(level,field,x,z);
        if(surface==RiverCurrentField.DRY||Math.abs(entity.getY()-surface)>4)return;
        long key=((long)(x>>4)<<32)^((z>>4)&0xffffffffL);var cache=CACHE.computeIfAbsent(level,l->new LinkedHashMap<>());
        var entry=cache.get(key);
        if(entry==null||entry.until<level.getGameTime()){
            var samples=new HashMap<Long,Integer>();
            var flow=RiverCurrentField.solve(x,z,(a,b)->samples.computeIfAbsent(((long)a<<32)^(b&0xffffffffL),k->waterHeight(level,field,a,b)));
            entry=new Cached(level.getGameTime()+600,flow);cache.put(key,entry);
            if(cache.size()>512)cache.remove(cache.keySet().iterator().next());
        }
        var flow=entry.flow;var velocity=entity.getDeltaMovement();
        if(velocity.x*flow.x()+velocity.z*flow.z()<.045){entity.push(flow.x()*.008,0,flow.z()*.008);entity.hasImpulse=true;
            if(entity instanceof net.minecraft.server.level.ServerPlayer&&entity.tickCount%5==0)entity.hurtMarked=true;
        }
    }
    private static int waterHeight(ServerLevel level,GeyserRegion field,int x,int z){
        var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);
        if(chunk!=null){
            int y=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15);
            var p=new BlockPos(x,y,z);
            return chunk.getFluidState(p).is(FluidTags.WATER)?y:RiverCurrentField.DRY;
        }
        // Metadata sampling extends the connected search beyond loaded terrain; never generate a chunk for currents.
        if(field!=null&&field.contains(x>>4,z>>4)){
            long seed=GeyserLayout.mix(level.getSeed()^0x474559534552L);
            return GeyserTerrain.height(seed,field,x,z)<level.getSeaLevel()-1?level.getSeaLevel()-1:RiverCurrentField.DRY;
        }
        var biome=level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(x>>2,level.getSeaLevel()>>2,z>>2,level.getChunkSource().randomState().sampler());
        return biome.is(BiomeTags.IS_RIVER)||biome.is(BiomeTags.IS_OCEAN)?level.getSeaLevel()-1:RiverCurrentField.DRY;
    }
    private RiverCurrents(){}
}
