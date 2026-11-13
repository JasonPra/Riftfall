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
        var biome=level.getBiome(pos);var field=GeyserBiomes.landscapeField(level,x>>4,z>>4);
        boolean zoneBiome=biome.unwrapKey().map(k->k.location().getNamespace().equals("riftfall")&&k.location().getPath().startsWith("geyser_")).orElse(false);
        long seed=GeyserLayout.mix(level.getSeed()^0x474559534552L);
        var underground=field==null?null:GeyserUndergroundRiver.of(seed,field).column(x,z);
        boolean below=underground!=null&&entity.getY()<=underground.roof()&&Math.abs(entity.getY()-underground.water())<4;
        boolean planned=field!=null&&(zoneBiome&&GeyserTerrain.column(seed,field,x,z).wet()||GeyserCoast.river(level,field,x,z));
        if(!biome.is(BiomeTags.IS_RIVER)&&!planned&&!below)return;
        int surface=below?underground.water():waterHeight(level,field,x,z);
        if(surface==RiverCurrentField.DRY||Math.abs(entity.getY()-surface)>4)return;
        RiverCurrentField.Flow selected;
        if(below)selected=new RiverCurrentField.Flow(underground.flowX(),underground.flowZ());
        else if(planned){
            if(GeyserShape.distance(field,x,z)<=0)selected=GeyserDrainage.of(field).flow(x,z);
            else selected=GeyserCoast.flow(level,field,x,z);
        }else{
            long key=((long)(x>>4)<<32)^((z>>4)&0xffffffffL);var cache=CACHE.computeIfAbsent(level,l->new LinkedHashMap<>());
            var entry=cache.get(key);
            if(entry==null||entry.until<level.getGameTime()){
                var samples=new HashMap<Long,Integer>();var sampledField=field;
                var flow=RiverCurrentField.solve(x,z,(a,b)->samples.computeIfAbsent(((long)a<<32)^(b&0xffffffffL),k->waterHeight(level,sampledField,a,b)));
                entry=new Cached(level.getGameTime()+600,flow);cache.put(key,entry);
                if(cache.size()>512)cache.remove(cache.keySet().iterator().next());
            }
            selected=entry.flow;
        }
        // Existing chunks may predate this plan, or a player may have blocked the channel.
        // Do not push into dry land or uphill water merely because metadata predicts a river.
        if(planned||below){
            int aheadX=x+(int)Math.round(selected.x()*4),aheadZ=z+(int)Math.round(selected.z()*4);
            int ahead;
            if(below){
                var cut=GeyserUndergroundRiver.of(seed,field).column(aheadX,aheadZ);ahead=cut!=null&&cut.wet()?cut.water():RiverCurrentField.DRY;
                var loaded=level.getChunkSource().getChunkNow(aheadX>>4,aheadZ>>4);
                if(ahead!=RiverCurrentField.DRY&&loaded!=null&&!loaded.getFluidState(new BlockPos(aheadX,ahead,aheadZ)).is(FluidTags.WATER))ahead=RiverCurrentField.DRY;
            }
            else ahead=waterHeight(level,field,aheadX,aheadZ);
            if(ahead==RiverCurrentField.DRY||ahead>surface)selected=new RiverCurrentField.Flow(0,0);
        }
        var flow=selected;var velocity=entity.getDeltaMovement();
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
            var column=GeyserTerrain.column(seed,field,x,z);
            return column.wet()?column.water():RiverCurrentField.DRY;
        }
        var biome=level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(x>>2,level.getSeaLevel()>>2,z>>2,level.getChunkSource().randomState().sampler());
        return biome.is(BiomeTags.IS_RIVER)||biome.is(BiomeTags.IS_OCEAN)?level.getSeaLevel()-1:RiverCurrentField.DRY;
    }
    private RiverCurrents(){}
}
