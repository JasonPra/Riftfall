package org.Azkerash.riftfall;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record BackstabActorPayload(int entityId,UUID uuid,int ticks,float yaw) implements CustomPacketPayload {
    public static final Type<BackstabActorPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"backstab_actor"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BackstabActorPayload> CODEC=new StreamCodec<>(){
        public BackstabActorPayload decode(RegistryFriendlyByteBuf b){return new BackstabActorPayload(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readFloat());}
        public void encode(RegistryFriendlyByteBuf b,BackstabActorPayload p){b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.ticks);b.writeFloat(p.yaw);}
    };
    public BackstabActorPayload {if(ticks<0 || ticks>BackstabSequence.GREATSWORD_ATTACK_TICKS || !Float.isFinite(yaw))throw new IllegalArgumentException("Invalid actor lock");}
    @Override public Type<BackstabActorPayload> type(){return TYPE;}
    static void send(Player p,int ticks,float yaw){PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new BackstabActorPayload(p.getId(),p.getUUID(),ticks,yaw));}
    static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,(packet,c)->{
        var entity=c.player().level().getEntity(packet.entityId);if(entity instanceof Player p && p.getUUID().equals(packet.uuid))BackstabActorLock.receive(p,packet.ticks,packet.yaw);
    });}
}
