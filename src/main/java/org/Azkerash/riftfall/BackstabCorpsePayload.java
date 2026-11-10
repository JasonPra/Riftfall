package org.Azkerash.riftfall;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Only synchronizes a settled face-down pose after the deferred death, including for players. */
public record BackstabCorpsePayload(int entityId,UUID uuid,int ticks,boolean great) implements CustomPacketPayload {
    public static final Type<BackstabCorpsePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"backstab_corpse"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BackstabCorpsePayload> CODEC=new StreamCodec<>(){
        public BackstabCorpsePayload decode(RegistryFriendlyByteBuf b){return new BackstabCorpsePayload(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readBoolean());}
        public void encode(RegistryFriendlyByteBuf b,BackstabCorpsePayload p){b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.ticks);b.writeBoolean(p.great);}
    };
    public BackstabCorpsePayload {if(ticks<0 || ticks>22)throw new IllegalArgumentException("Invalid corpse hold duration");}
    @Override public Type<BackstabCorpsePayload> type(){return TYPE;}
    static void send(LivingEntity entity,int ticks,boolean great){PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,new BackstabCorpsePayload(entity.getId(),entity.getUUID(),ticks,great));}
    static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(TYPE,CODEC,(packet,context)->{
            var entity=context.player().level().getEntity(packet.entityId);
            if(entity instanceof LivingEntity living && entity.getUUID().equals(packet.uuid))BackstabSequence.clientCorpseHold(living,packet.ticks,packet.great);
        });
    }
}
